package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.PersonalizedOfferDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Offer;
import com.crimsonlogic.busticketbooking.entity.PersonalizedUserOffer;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserPreference;
import com.crimsonlogic.busticketbooking.enums.DiscountType;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.OfferRepository;
import com.crimsonlogic.busticketbooking.repository.PersonalizedUserOfferRepository;
import com.crimsonlogic.busticketbooking.repository.UserPreferenceRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiOfferGenerationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiOfferGenerationServiceImpl implements AiOfferGenerationService {

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final BookingRepository bookingRepository;
    private final OfferRepository offerRepository;
    private final PersonalizedUserOfferRepository personalizedUserOfferRepository;

    public AiOfferGenerationServiceImpl(ChatClient.Builder chatClientBuilder,
                                        UserRepository userRepository,
                                        UserPreferenceRepository userPreferenceRepository,
                                        BookingRepository bookingRepository,
                                        OfferRepository offerRepository,
                                        PersonalizedUserOfferRepository personalizedUserOfferRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.bookingRepository = bookingRepository;
        this.offerRepository = offerRepository;
        this.personalizedUserOfferRepository = personalizedUserOfferRepository;
    }

    @Override
    @Transactional
    public List<PersonalizedOfferDTO> getOrGenerateOffersForUser(String userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<PersonalizedUserOffer> existingOffers = personalizedUserOfferRepository.findByUser_UserId(userId);
        
        boolean hasActiveOffer = existingOffers.stream()
                .anyMatch(po -> po.getOffer().getValidUntil().isAfter(LocalDateTime.now()) && !isOfferUsed(userId, po.getOffer().getOfferCode()));

        if (!hasActiveOffer) {
            generateNewOffer(user);
            existingOffers = personalizedUserOfferRepository.findByUser_UserId(userId);
        }

        List<PersonalizedOfferDTO> result = new ArrayList<>();
        for (PersonalizedUserOffer po : existingOffers) {
            PersonalizedOfferDTO dto = new PersonalizedOfferDTO();
            Offer offer = po.getOffer();
            dto.setOfferCode(offer.getOfferCode());
            dto.setDescription(offer.getDescription());
            dto.setDiscountType(offer.getDiscountType().name());
            dto.setDiscountValue(offer.getDiscountValue());
            dto.setMinimumBookingAmount(offer.getMinimumBookingAmount());
            dto.setValidFrom(offer.getValidFrom());
            dto.setValidUntil(offer.getValidUntil());
            dto.setUsed(isOfferUsed(userId, offer.getOfferCode()));
            result.add(dto);
        }

        return result;
    }

    private void generateNewOffer(User user) {
        // 1. Gather context
        List<Booking> userBookings = bookingRepository.findByBookedByUser_UserId(user.getUserId());
        int totalBookings = userBookings.size();
        
        Optional<UserPreference> prefOpt = userPreferenceRepository.findByUser_UserId(user.getUserId());
        String preferredBusType = prefOpt.map(UserPreference::getPreferredBusType).orElse("Any");
        
        // 2. Prepare LLM prompt
        BeanOutputConverter<AiOfferResponse> converter = new BeanOutputConverter<>(AiOfferResponse.class);
        String format = converter.getFormat();

        String systemPrompt = String.format(
                "You are an AI promotional engine for a bus ticket booking system. " +
                "Generate a personalized offer based on the user's travel history: " +
                "Total Bookings: %d. Preferred Bus Type: %s. " +
                "If they are a frequent traveler (>5 bookings), give a percentage discount (e.g. 15%%) or flat 200 discount. " +
                "If they are new, give a welcoming flat discount (e.g. 100 flat). " +
                "Write a short, engaging description for the user (max 100 chars). " +
                "Provide the result in the exact requested format.\n\n%s",
                totalBookings, preferredBusType, format);

        // 3. Call LLM
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user("Generate offer for user.")
                .call()
                .content();

        AiOfferResponse aiOffer = converter.convert(response);
        if (aiOffer == null) {
            throw new IllegalStateException("Failed to generate AI offer.");
        }

        // 4. Save Offer
        Offer offer = new Offer();
        offer.setOfferCode("AI" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        offer.setDescription(aiOffer.getDescription());
        offer.setDiscountType(DiscountType.valueOf(aiOffer.getDiscountType().toUpperCase()));
        offer.setDiscountValue(aiOffer.getDiscountValue());
        if (aiOffer.getMinimumBookingAmount() != null) {
            offer.setMinimumBookingAmount(aiOffer.getMinimumBookingAmount());
        } else {
            offer.setMinimumBookingAmount(BigDecimal.ZERO);
        }
        
        if (DiscountType.PERCENTAGE.equals(offer.getDiscountType())) {
            offer.setMaximumDiscountAmount(new BigDecimal("500.00")); // Cap at 500
        }
        
        offer.setValidFrom(LocalDateTime.now());
        offer.setValidUntil(LocalDateTime.now().plusDays(30)); // Valid for 30 days
        offer.setIsActive(true);
        
        offerRepository.save(offer);

        // 5. Link Offer to User
        PersonalizedUserOffer personalizedOffer = new PersonalizedUserOffer();
        personalizedOffer.setUser(user);
        personalizedOffer.setOffer(offer);
        personalizedUserOfferRepository.save(personalizedOffer);
    }

    private boolean isOfferUsed(String userId, String offerCode) {
        return bookingRepository.findByBookedByUser_UserId(userId).stream()
                .anyMatch(b -> offerCode.equalsIgnoreCase(b.getOfferCodeUsed()));
    }

    // Inner class for LLM JSON output mapping
    public static class AiOfferResponse {
        private String description;
        private String discountType; // PERCENTAGE or FIXED_AMOUNT
        private BigDecimal discountValue;
        private BigDecimal minimumBookingAmount;

        public AiOfferResponse() {}

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDiscountType() { return discountType; }
        public void setDiscountType(String discountType) { this.discountType = discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
        public BigDecimal getMinimumBookingAmount() { return minimumBookingAmount; }
        public void setMinimumBookingAmount(BigDecimal minimumBookingAmount) { this.minimumBookingAmount = minimumBookingAmount; }
    }
}
