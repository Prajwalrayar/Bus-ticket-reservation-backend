package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Table(name = "search_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {

    @Id
    @Column(
            name = "search_history_id",
            length = 30,
            updatable = false
    )
    private String searchHistoryId;

    /*
     * Source location entered by the user.
     */
    @Column(
            name = "source",
            nullable = false,
            length = 100
    )
    private String source;

    /*
     * Destination location entered by the user.
     */
    @Column(
            name = "destination",
            nullable = false,
            length = 100
    )
    private String destination;

    /*
     * Date for which buses were searched.
     */
    @Column(
            name = "travel_date",
            nullable = false
    )
    private LocalDate travelDate;

    /*
     * Number of passengers specified in the search.
     */
    @Column(
            name = "passenger_count",
            nullable = false
    )
    private Integer passengerCount;

    /*
     * Indicates whether the search originated from
     * a natural-language/AI search.
     */
    @Column(
            name = "is_natural_language",
            nullable = false
    )
    private Boolean isNaturalLanguage = false;

    /*
     * Original natural-language query, if applicable.
     *
     * Example:
     * "Find a sleeper bus from Bangalore to Hyderabad tomorrow"
     */
    @Lob
    @Column(
            name = "natural_language_query",
            columnDefinition = "TEXT"
    )
    private String naturalLanguageQuery;

    /*
     * Time at which the search was performed.
     */
    @CreationTimestamp
    @Column(
            name = "searched_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime searchedAt;

    /*
     * User who performed the search.
     *
     * Nullable so searches can also be recorded for
     * users who are not logged in.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void generateId() {
        if (this.searchHistoryId == null) {
            this.searchHistoryId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_SEARCH_HISTORY);
        }
    }
}
