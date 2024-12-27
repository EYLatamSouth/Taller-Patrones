package org.ey;

import org.ey.dao.PortfolioDAO;
import org.ey.enums.PortfolioStatus;
import org.ey.enums.ResolutionEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PolicyProcessor {
    final PortfolioDAO dao;
    boolean useSimplePolicies;

    public PolicyProcessor(final PortfolioDAO dao, boolean useSimplePolicies){
        this.dao = dao;
        this.useSimplePolicies = useSimplePolicies;
    }

    public final PortfolioDAO getDao() {
        return dao;
    }

    public void setUseSimplePolicies(boolean flag) {
        this.useSimplePolicies = flag;
    }

    // ### EJEMPLO. EN LOS TEST SE USARÁ EL METODO "process" ###
    public void processEjemplo(List<Map<String, Object>> policies, List<Map<String, String>> movements){
        System.out.println(movements);
        System.out.println(policies);

        movements.forEach(
                movement -> {
                    var id = movement.get("carteraId");
                    var oldStatus = dao.getPortfolioStatus(Long.parseLong(id));
                    var newHardcodedStatus =  PortfolioStatus.VIP;
                    //Ejemplo. El estado nuevo debe ser el resultado de procesar reglas de políticas.
                    System.out.println("Cartera Id: " + id +
                            ", Status Viejo: " + oldStatus + ", Status Nuevo: " + newHardcodedStatus);
                }
        );

    }

    public void process(List<Map<String, Object>> policies, List<Map<String, String>> movements){
        try {
            for (var movement : movements) {
                boolean terminated = false;
                var eventList = ResolutionEvent.getAllEventsForProcess();
                for (var policy : policies) {
                    if(terminated) {break;}
                        boolean evaluation;
                        var fieldValue = movement.get(policy.get("field"));
                        var compareToValue = (String) policy.get("compareToValue");
                        Double fieldValueAsNumber = null;
                        Double compareToValueAsNumber = null;
                        boolean isNumber = true;
                        try {
                            fieldValueAsNumber = Double.parseDouble(fieldValue);
                            compareToValueAsNumber = Double.parseDouble(compareToValue);
                        } catch (NumberFormatException ignored) {
                            isNumber = false;
                        }

                        if (isNumber) {
                            switch ((String) policy.get("comparator")) {
                                case "greater_than" -> evaluation = fieldValueAsNumber > compareToValueAsNumber;
                                case "equal" -> evaluation = fieldValueAsNumber.equals(compareToValueAsNumber);
                                case "greater_equal" -> evaluation = fieldValueAsNumber >= compareToValueAsNumber;
                                case "less_than" -> evaluation = fieldValueAsNumber < compareToValueAsNumber;
                                case "less_equal" -> evaluation = fieldValueAsNumber <= compareToValueAsNumber;
                                case null, default -> evaluation = false;

                            }
                        } else {
                            switch ((String) policy.get("comparator")) {
                                //STRING LITERALS ARE ONLY COMPARED BY EQUALS
                                case "equal" -> evaluation = fieldValue.equals(compareToValue);
                                case null, default -> evaluation = false;

                            }
                        }
                        if (evaluation) {
                            switch ((String) policy.get("operator")) {
                                case "NOT":
                                    eventList = eventList.stream()
                                            .filter(e -> !((List<String>) policy.get("events")).contains(e.toString())).toList();
                                    break;
                                case "ONLY":
                                    eventList = ((List<String>) policy.get("events")).stream()
                                            .map(s -> ResolutionEvent.valueOf(s)).toList();
                                    break;
                                case "RESULT":
                                    eventList = ((List<String>) policy.get("events")).stream()
                                            .map(s -> ResolutionEvent.valueOf(s)).toList();
                                    terminated = true;
                                    break;
                                case null, default:
                            }
                        }
                }
                var determinedEvent = eventList.get(0);
                var carteraId = Long.parseLong(movement.get("carteraId"));
                var currentState = dao.getPortfolioStatus(carteraId);

                switch (determinedEvent) {
                    case BEAR:
                        if(currentState != PortfolioStatus.CLOSED){
                            dao.savePortfolioStatus(carteraId,  PortfolioStatus.EMPTY);
                        }
                        break;
                    case BULL:
                        if(currentState == PortfolioStatus.EMPTY || currentState == PortfolioStatus.DEFENSIVE){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.ACTIVE);
                        }
                        else if(currentState != PortfolioStatus.CLOSED){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.VIP);
                        }
                        break;

                    case DEBT_DEFAULT:
                        if(currentState != PortfolioStatus.CLOSED){
                            if(currentState != PortfolioStatus.EMPTY && currentState != PortfolioStatus.DEFENSIVE){
                                dao.savePortfolioStatus(carteraId, PortfolioStatus.DEFENSIVE);
                            }
                            else if(currentState == PortfolioStatus.DEFENSIVE){
                                dao.savePortfolioStatus(carteraId, PortfolioStatus.EMPTY);
                            }
                        }

                        break;

                    case EXTREME_RISK:
                        dao.savePortfolioStatus(carteraId, PortfolioStatus.CLOSED);
                        break;

                    case MARKET_COLLAPSE:
                        if(currentState != PortfolioStatus.VIP && currentState != PortfolioStatus.CLOSED){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.EMPTY);
                        }
                        else if(currentState == PortfolioStatus.VIP){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.CLOSED);
                        }
                        break;
                    case OUT_OF_INVESTORS:
                        if(currentState ==  PortfolioStatus.ACTIVE){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.DEFENSIVE);
                        }
                        else if(currentState ==  PortfolioStatus.DEFENSIVE){
                            dao.savePortfolioStatus(carteraId, PortfolioStatus.CLOSED);
                        }
                        break;
                    case null, default:
                        dao.savePortfolioStatus(carteraId, currentState);
                }

                System.out.println("movementId:" + carteraId + ", event:" +determinedEvent + ", oldState:" + currentState + ", newState:" + dao.getPortfolioStatus(carteraId) );



            }
        } catch (Exception e){
            System.out.println("Error");
            e.printStackTrace();
        }
        /*

               currentState = getState(m)
               switch (determinedEvent)


                    case OUT_OF_INVESTORS
                        if(currentState == ACTIVE) then
                           setState(m, DEFENSIVE)
                        if(currentState == DEFENSIVE) then
                           setState(m, CLOSED)
                    case MARKET_COLLAPSE
                        if(currentState != VIP || currentState != CLOSED) then
                           setState(m, EMPTY)
                        if(currentState == VIP) then
                           setState(m, CLOSED)
                    default:

         continue foreach

        */


    }
}
