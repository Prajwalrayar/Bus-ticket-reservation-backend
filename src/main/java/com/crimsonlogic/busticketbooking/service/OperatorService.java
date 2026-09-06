package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.entity.Operator;

import java.util.List;
import java.util.Map;

public interface OperatorService {

    Operator createOperator(Operator operator);

    Operator getOperatorByCompanyName(String companyName);

    List<Operator> getAllOperators();

    Operator updateOperator(String companyName, Operator operator);

    void approveOperator(String companyName);

    void deactivateOperator(String companyName);
}
