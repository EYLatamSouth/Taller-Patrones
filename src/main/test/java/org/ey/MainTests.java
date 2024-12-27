package org.ey;

import org.ey.enums.PortfolioStatus;
import org.ey.utils.UtilsProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MainTests {

    @ParameterizedTest
    @MethodSource("expectedResultsComplete")
    public void testPolicyProcessor(final Long id , final Map<PortfolioStatus, PortfolioStatus> expectedResults){
        testForPolicy(id, expectedResults,
                "src/main/test/resources/test01/policies.json", false);
    }

    @ParameterizedTest
    @MethodSource("expectedResultsSimple")
    public void testPolicyProcessorSimple(final Long id , final Map<PortfolioStatus, PortfolioStatus> expectedResults){
        testForPolicy(id, expectedResults,
                "src/main/test/resources/test01/policies-simple.json", true);

    }

    private void testForPolicy(final Long id, final Map<PortfolioStatus, PortfolioStatus> expectedResults,
                               final String policiesPath, final boolean isSimple){
        var testDao = new TestPortfolioDao();
        final PolicyProcessor policyProcessor = new PolicyProcessor(testDao, isSimple);

        final List<Map<String, Object>> policies =
                UtilsProxy.readPolicies(policiesPath);
        final List<Map<String, String>> movements=
                UtilsProxy.readMovements("src/main/test/resources/test01/portfolio-movements.txt");


        expectedResults.forEach(
                (oldStatus, expectedNewStatus) -> {
                    testDao.savePortfolioStatus(id, oldStatus);
                    System.out.println("Testing id: " + id + ", setStatus:" + oldStatus + ",expectedNew: " + expectedNewStatus);
                    policyProcessor.process(
                            policies,
                            movements
                    );
                    Assertions.assertEquals(expectedNewStatus, testDao.getPortfolioStatusRaw(id));
                });


        Assertions.assertEquals(0, testDao.getTotalExceptions());
        Assertions.assertTrue(testDao.getInvocations("getPortfolioStatus") > 0);
        Assertions.assertTrue(testDao.getInvocations("savePortfolioStatus") > 0);
        Assertions.assertFalse(testDao.notInteracted());

    }

    private static Stream<Arguments> expectedResultsComplete() {
        //1|1200.00|APPLE|STOCK|TRUE -> RESUELVE BULL
        var results01 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.ACTIVE,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.ACTIVE,
                PortfolioStatus.VIP, PortfolioStatus.VIP,
                PortfolioStatus.ACTIVE, PortfolioStatus.VIP);

        //2|10000.00|TESLA|STOCK|FALSE -> RESUELVE BEAR
        var results02 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.EMPTY,
                PortfolioStatus.ACTIVE, PortfolioStatus.EMPTY);

        //3|200.0|LELIQ|BOND|FALSE -> RESUELVE AUDIT_RISK
        var results03 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.VIP, PortfolioStatus.VIP,
                PortfolioStatus.ACTIVE, PortfolioStatus.ACTIVE);

        //4|100000.00|DOLLAR|FUTURE|FALSE -> RESUELVE MARKET_COLLAPSE
        var results04 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED ,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.CLOSED ,
                PortfolioStatus.ACTIVE, PortfolioStatus.EMPTY);

        //5|100000.00|OIL|FUTURE|TRUE -> RESUELVE OUT_OF_INVESTORS
        var results05 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.CLOSED,
                PortfolioStatus.VIP, PortfolioStatus.VIP,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        //6|150000.00|LEBAD|BOND|FALSE -> RESUELVE AUDIT_RISK
        var results06 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.VIP, PortfolioStatus.VIP,
                PortfolioStatus.ACTIVE, PortfolioStatus.ACTIVE);

        //7|20000000.00|GOLD|COMODITY|FALSE ->RESUELVE DEBT_DEFAULT
        var results07 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        return Stream.of(
                Arguments.of(1L, results01),
                Arguments.of(2L, results02),
                Arguments.of(3L, results03),
                Arguments.of(4L, results04),
                Arguments.of(5L, results05),
                Arguments.of(6L, results06),
                Arguments.of(7L, results07)
        );
    }

    private static Stream<Arguments> expectedResultsSimple() {
        //1|1200.00|APPLE|STOCK|TRUE -> DEBT_DEFAULT
        var results01 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        //2|10000.00|TESLA|STOCK|FALSE -> DEBT_DEFAULT
        var results02 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        //3|200.0|LELIQ|BOND|FALSE -> EXTREME_RISK
        var results03 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.CLOSED,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.CLOSED,
                PortfolioStatus.VIP, PortfolioStatus.CLOSED,
                PortfolioStatus.ACTIVE, PortfolioStatus.CLOSED);

        //4|100000.00|DOLLAR|FUTURE|FALSE -> DEBT_DEFAULT
        var results04 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        //5|100000.00|OIL|FUTURE|TRUE ->DEBT_DEFAULT
        var results05 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED,
                PortfolioStatus.EMPTY, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.DEFENSIVE,
                PortfolioStatus.ACTIVE, PortfolioStatus.DEFENSIVE);

        //6|150000.00|LEBAD|BOND|FALSE -> MARKET_COLLAPSE
        var results06 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED ,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.CLOSED ,
                PortfolioStatus.ACTIVE, PortfolioStatus.EMPTY);

        //7|20000000.00|GOLD|COMODITY|FALSE -> MARKET_COLLAPSE
        var results07 = Map.of(
                PortfolioStatus.CLOSED, PortfolioStatus.CLOSED ,
                PortfolioStatus.EMPTY, PortfolioStatus.EMPTY,
                PortfolioStatus.DEFENSIVE, PortfolioStatus.EMPTY,
                PortfolioStatus.VIP, PortfolioStatus.CLOSED ,
                PortfolioStatus.ACTIVE, PortfolioStatus.EMPTY);

        return Stream.of(
                Arguments.of(1L, results01),
                Arguments.of(2L, results02),
                Arguments.of(3L, results03),
                Arguments.of(4L, results04),
                Arguments.of(5L, results05),
                Arguments.of(6L, results06),
                Arguments.of(7L, results07)
        );
    }

}
