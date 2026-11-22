package carrs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Total assistance distributed (Completed payments) in a period. */
public class PaymentSummary {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalDistributed;
    private final int paymentCount;
    private final Map<BenefitType, BigDecimal> totalByBenefitType;

    public PaymentSummary(LocalDate fromDate, LocalDate toDate, BigDecimal totalDistributed,
                          int paymentCount, Map<BenefitType, BigDecimal> totalByBenefitType) {
        this.fromDate = fromDate; this.toDate = toDate; this.totalDistributed = totalDistributed;
        this.paymentCount = paymentCount; this.totalByBenefitType = totalByBenefitType;
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public BigDecimal getTotalDistributed() { return totalDistributed; }
    public int getPaymentCount() { return paymentCount; }
    public Map<BenefitType, BigDecimal> getTotalByBenefitType() { return totalByBenefitType; }
}
