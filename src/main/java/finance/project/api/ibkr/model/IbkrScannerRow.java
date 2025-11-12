package finance.project.api.ibkr.model;

import com.ib.client.ContractDetails;

/**
 * Immutable representation of a single scanner row returned by IBKR.
 */
public record IbkrScannerRow(int rank,
                             ContractDetails contractDetails,
                             String distance,
                             String benchmark,
                             String projection,
                             String legs) {
}
