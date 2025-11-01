package finance.project.api.adapter;

import com.ib.client.*;
import com.ib.client.protobuf.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class IbkrWrapperAdapter implements EWrapper {
    @Override
    public void tickPrice(int i, int i1, double v, TickAttrib tickAttrib) {

    }

    @Override
    public void tickSize(int i, int i1, Decimal decimal) {

    }

    @Override
    public void tickOptionComputation(int i, int i1, int i2, double v, double v1, double v2, double v3, double v4, double v5, double v6, double v7) {

    }

    @Override
    public void tickGeneric(int i, int i1, double v) {

    }

    @Override
    public void tickString(int i, int i1, String s) {

    }

    @Override
    public void tickEFP(int i, int i1, double v, String s, double v1, int i2, String s1, double v2, double v3) {

    }

    @Override
    public void orderStatus(int i, String s, Decimal decimal, Decimal decimal1, double v, long l, int i1, double v1, int i2, String s1, double v2) {

    }

    @Override
    public void openOrder(int i, Contract contract, Order order, OrderState orderState) {

    }

    @Override
    public void openOrderEnd() {

    }

    @Override
    public void updateAccountValue(String s, String s1, String s2, String s3) {

    }

    @Override
    public void updatePortfolio(Contract contract, Decimal decimal, double v, double v1, double v2, double v3, double v4, String s) {

    }

    @Override
    public void updateAccountTime(String s) {

    }

    @Override
    public void accountDownloadEnd(String s) {

    }

    @Override
    public void nextValidId(int i) {

    }

    @Override
    public void contractDetails(int i, ContractDetails contractDetails) {

    }

    @Override
    public void bondContractDetails(int i, ContractDetails contractDetails) {

    }

    @Override
    public void contractDetailsEnd(int i) {

    }

    @Override
    public void execDetails(int i, Contract contract, Execution execution) {

    }

    @Override
    public void execDetailsEnd(int i) {

    }

    @Override
    public void updateMktDepth(int i, int i1, int i2, int i3, double v, Decimal decimal) {

    }

    @Override
    public void updateMktDepthL2(int i, int i1, String s, int i2, int i3, double v, Decimal decimal, boolean b) {

    }

    @Override
    public void updateNewsBulletin(int i, int i1, String s, String s1) {

    }

    @Override
    public void managedAccounts(String s) {

    }

    @Override
    public void receiveFA(int i, String s) {

    }

    @Override
    public void historicalData(int i, Bar bar) {

    }

    @Override
    public void scannerParameters(String s) {

    }

    @Override
    public void scannerData(int i, int i1, ContractDetails contractDetails, String s, String s1, String s2, String s3) {

    }

    @Override
    public void scannerDataEnd(int i) {

    }

    @Override
    public void realtimeBar(int i, long l, double v, double v1, double v2, double v3, Decimal decimal, Decimal decimal1, int i1) {

    }

    @Override
    public void currentTime(long l) {

    }

    @Override
    public void fundamentalData(int i, String s) {

    }

    @Override
    public void deltaNeutralValidation(int i, DeltaNeutralContract deltaNeutralContract) {

    }

    @Override
    public void tickSnapshotEnd(int i) {

    }

    @Override
    public void marketDataType(int i, int i1) {

    }

    @Override
    public void commissionAndFeesReport(CommissionAndFeesReport commissionAndFeesReport) {

    }

    @Override
    public void position(String s, Contract contract, Decimal decimal, double v) {

    }

    public abstract void position(String account, Contract contract, double pos, double avgCost);

    @Override
    public void positionEnd() {

    }

    @Override
    public void accountSummary(int i, String s, String s1, String s2, String s3) {

    }

    @Override
    public void accountSummaryEnd(int i) {

    }

    @Override
    public void verifyMessageAPI(String s) {

    }

    @Override
    public void verifyCompleted(boolean b, String s) {

    }

    @Override
    public void verifyAndAuthMessageAPI(String s, String s1) {

    }

    @Override
    public void verifyAndAuthCompleted(boolean b, String s) {

    }

    @Override
    public void displayGroupList(int i, String s) {

    }

    @Override
    public void displayGroupUpdated(int i, String s) {

    }

    @Override
    public void error(Exception e) {

    }

    @Override
    public void error(String s) {

    }

    @Override
    public void error(int i, long l, int i1, String s, String s1) {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectAck() {

    }

    @Override
    public void positionMulti(int i, String s, String s1, Contract contract, Decimal decimal, double v) {

    }

    @Override
    public void positionMultiEnd(int i) {

    }

    @Override
    public void accountUpdateMulti(int i, String s, String s1, String s2, String s3, String s4) {

    }

    @Override
    public void accountUpdateMultiEnd(int i) {

    }

    @Override
    public void securityDefinitionOptionalParameter(int i, String s, int i1, String s1, String s2, Set<String> set, Set<Double> set1) {

    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int i) {

    }

    @Override
    public void softDollarTiers(int i, SoftDollarTier[] softDollarTiers) {

    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {

    }

    @Override
    public void symbolSamples(int i, ContractDescription[] contractDescriptions) {

    }

    @Override
    public void historicalDataEnd(int i, String s, String s1) {

    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {

    }

    @Override
    public void tickNews(int i, long l, String s, String s1, String s2, String s3) {

    }

    @Override
    public void smartComponents(int i, Map<Integer, Map.Entry<String, Character>> map) {

    }

    @Override
    public void tickReqParams(int i, double v, String s, int i1) {

    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {

    }

    @Override
    public void newsArticle(int i, int i1, String s) {

    }

    @Override
    public void historicalNews(int i, String s, String s1, String s2, String s3) {

    }

    @Override
    public void historicalNewsEnd(int i, boolean b) {

    }

    @Override
    public void headTimestamp(int i, String s) {

    }

    @Override
    public void histogramData(int i, List<HistogramEntry> list) {

    }

    @Override
    public void historicalDataUpdate(int i, Bar bar) {

    }

    @Override
    public void rerouteMktDataReq(int i, int i1, String s) {

    }

    @Override
    public void rerouteMktDepthReq(int i, int i1, String s) {

    }

    @Override
    public void marketRule(int i, PriceIncrement[] priceIncrements) {

    }

    @Override
    public void pnl(int i, double v, double v1, double v2) {

    }

    @Override
    public void pnlSingle(int i, Decimal decimal, double v, double v1, double v2, double v3) {

    }

    @Override
    public void historicalTicks(int i, List<HistoricalTick> list, boolean b) {

    }

    @Override
    public void historicalTicksBidAsk(int i, List<HistoricalTickBidAsk> list, boolean b) {

    }

    @Override
    public void historicalTicksLast(int i, List<HistoricalTickLast> list, boolean b) {

    }

    @Override
    public void tickByTickAllLast(int i, int i1, long l, double v, Decimal decimal, TickAttribLast tickAttribLast, String s, String s1) {

    }

    @Override
    public void tickByTickBidAsk(int i, long l, double v, double v1, Decimal decimal, Decimal decimal1, TickAttribBidAsk tickAttribBidAsk) {

    }

    @Override
    public void tickByTickMidPoint(int i, long l, double v) {

    }

    @Override
    public void orderBound(long l, int i, int i1) {

    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {

    }

    @Override
    public void completedOrdersEnd() {

    }

    @Override
    public void replaceFAEnd(int i, String s) {

    }

    @Override
    public void wshMetaData(int i, String s) {

    }

    @Override
    public void wshEventData(int i, String s) {

    }

    @Override
    public void historicalSchedule(int i, String s, String s1, String s2, List<HistoricalSession> list) {

    }

    @Override
    public void userInfo(int i, String s) {

    }

    @Override
    public void currentTimeInMillis(long l) {

    }

    @Override
    public void orderStatusProtoBuf(OrderStatusProto.OrderStatus orderStatus) {

    }

    @Override
    public void openOrderProtoBuf(OpenOrderProto.OpenOrder openOrder) {

    }

    @Override
    public void openOrdersEndProtoBuf(OpenOrdersEndProto.OpenOrdersEnd openOrdersEnd) {

    }

    @Override
    public void errorProtoBuf(ErrorMessageProto.ErrorMessage errorMessage) {

    }

    @Override
    public void execDetailsProtoBuf(ExecutionDetailsProto.ExecutionDetails executionDetails) {

    }

    @Override
    public void execDetailsEndProtoBuf(ExecutionDetailsEndProto.ExecutionDetailsEnd executionDetailsEnd) {

    }

    @Override
    public void completedOrderProtoBuf(CompletedOrderProto.CompletedOrder completedOrder) {

    }

    @Override
    public void completedOrdersEndProtoBuf(CompletedOrdersEndProto.CompletedOrdersEnd completedOrdersEnd) {

    }

    @Override
    public void orderBoundProtoBuf(OrderBoundProto.OrderBound orderBound) {

    }

    @Override
    public void contractDataProtoBuf(ContractDataProto.ContractData contractData) {

    }

    @Override
    public void bondContractDataProtoBuf(ContractDataProto.ContractData contractData) {

    }

    @Override
    public void contractDataEndProtoBuf(ContractDataEndProto.ContractDataEnd contractDataEnd) {

    }

    @Override
    public void tickPriceProtoBuf(TickPriceProto.TickPrice tickPrice) {

    }

    @Override
    public void tickSizeProtoBuf(TickSizeProto.TickSize tickSize) {

    }

    @Override
    public void tickOptionComputationProtoBuf(TickOptionComputationProto.TickOptionComputation tickOptionComputation) {

    }

    @Override
    public void tickGenericProtoBuf(TickGenericProto.TickGeneric tickGeneric) {

    }

    @Override
    public void tickStringProtoBuf(TickStringProto.TickString tickString) {

    }

    @Override
    public void tickSnapshotEndProtoBuf(TickSnapshotEndProto.TickSnapshotEnd tickSnapshotEnd) {

    }

    @Override
    public void updateMarketDepthProtoBuf(MarketDepthProto.MarketDepth marketDepth) {

    }

    @Override
    public void updateMarketDepthL2ProtoBuf(MarketDepthL2Proto.MarketDepthL2 marketDepthL2) {

    }

    @Override
    public void marketDataTypeProtoBuf(MarketDataTypeProto.MarketDataType marketDataType) {

    }

    @Override
    public void tickReqParamsProtoBuf(TickReqParamsProto.TickReqParams tickReqParams) {

    }

    @Override
    public void updateAccountValueProtoBuf(AccountValueProto.AccountValue accountValue) {

    }

    @Override
    public void updatePortfolioProtoBuf(PortfolioValueProto.PortfolioValue portfolioValue) {

    }

    @Override
    public void updateAccountTimeProtoBuf(AccountUpdateTimeProto.AccountUpdateTime accountUpdateTime) {

    }

    @Override
    public void accountDataEndProtoBuf(AccountDataEndProto.AccountDataEnd accountDataEnd) {

    }

    @Override
    public void managedAccountsProtoBuf(ManagedAccountsProto.ManagedAccounts managedAccounts) {

    }

    @Override
    public void positionProtoBuf(PositionProto.Position position) {

    }

    @Override
    public void positionEndProtoBuf(PositionEndProto.PositionEnd positionEnd) {

    }

    @Override
    public void accountSummaryProtoBuf(AccountSummaryProto.AccountSummary accountSummary) {

    }

    @Override
    public void accountSummaryEndProtoBuf(AccountSummaryEndProto.AccountSummaryEnd accountSummaryEnd) {

    }

    @Override
    public void positionMultiProtoBuf(PositionMultiProto.PositionMulti positionMulti) {

    }

    @Override
    public void positionMultiEndProtoBuf(PositionMultiEndProto.PositionMultiEnd positionMultiEnd) {

    }

    @Override
    public void accountUpdateMultiProtoBuf(AccountUpdateMultiProto.AccountUpdateMulti accountUpdateMulti) {

    }

    @Override
    public void accountUpdateMultiEndProtoBuf(AccountUpdateMultiEndProto.AccountUpdateMultiEnd accountUpdateMultiEnd) {

    }

    @Override
    public void historicalDataProtoBuf(HistoricalDataProto.HistoricalData historicalData) {

    }

    @Override
    public void historicalDataUpdateProtoBuf(HistoricalDataUpdateProto.HistoricalDataUpdate historicalDataUpdate) {

    }

    @Override
    public void historicalDataEndProtoBuf(HistoricalDataEndProto.HistoricalDataEnd historicalDataEnd) {

    }

    @Override
    public void realTimeBarTickProtoBuf(RealTimeBarTickProto.RealTimeBarTick realTimeBarTick) {

    }

    @Override
    public void headTimestampProtoBuf(HeadTimestampProto.HeadTimestamp headTimestamp) {

    }

    @Override
    public void histogramDataProtoBuf(HistogramDataProto.HistogramData histogramData) {

    }

    @Override
    public void historicalTicksProtoBuf(HistoricalTicksProto.HistoricalTicks historicalTicks) {

    }

    @Override
    public void historicalTicksBidAskProtoBuf(HistoricalTicksBidAskProto.HistoricalTicksBidAsk historicalTicksBidAsk) {

    }

    @Override
    public void historicalTicksLastProtoBuf(HistoricalTicksLastProto.HistoricalTicksLast historicalTicksLast) {

    }

    @Override
    public void tickByTickDataProtoBuf(TickByTickDataProto.TickByTickData tickByTickData) {

    }

    @Override
    public void updateNewsBulletinProtoBuf(NewsBulletinProto.NewsBulletin newsBulletin) {

    }

    @Override
    public void newsArticleProtoBuf(NewsArticleProto.NewsArticle newsArticle) {

    }

    @Override
    public void newsProvidersProtoBuf(NewsProvidersProto.NewsProviders newsProviders) {

    }

    @Override
    public void historicalNewsProtoBuf(HistoricalNewsProto.HistoricalNews historicalNews) {

    }

    @Override
    public void historicalNewsEndProtoBuf(HistoricalNewsEndProto.HistoricalNewsEnd historicalNewsEnd) {

    }

    @Override
    public void wshMetaDataProtoBuf(WshMetaDataProto.WshMetaData wshMetaData) {

    }

    @Override
    public void wshEventDataProtoBuf(WshEventDataProto.WshEventData wshEventData) {

    }

    @Override
    public void tickNewsProtoBuf(TickNewsProto.TickNews tickNews) {

    }

    @Override
    public void scannerParametersProtoBuf(ScannerParametersProto.ScannerParameters scannerParameters) {

    }

    @Override
    public void scannerDataProtoBuf(ScannerDataProto.ScannerData scannerData) {

    }

    @Override
    public void fundamentalsDataProtoBuf(FundamentalsDataProto.FundamentalsData fundamentalsData) {

    }

    @Override
    public void pnlProtoBuf(PnLProto.PnL pnL) {

    }

    @Override
    public void pnlSingleProtoBuf(PnLSingleProto.PnLSingle pnLSingle) {

    }

    @Override
    public void receiveFAProtoBuf(ReceiveFAProto.ReceiveFA receiveFA) {

    }

    @Override
    public void replaceFAEndProtoBuf(ReplaceFAEndProto.ReplaceFAEnd replaceFAEnd) {

    }

    @Override
    public void commissionAndFeesReportProtoBuf(CommissionAndFeesReportProto.CommissionAndFeesReport commissionAndFeesReport) {

    }

    @Override
    public void historicalScheduleProtoBuf(HistoricalScheduleProto.HistoricalSchedule historicalSchedule) {

    }

    @Override
    public void rerouteMarketDataRequestProtoBuf(RerouteMarketDataRequestProto.RerouteMarketDataRequest rerouteMarketDataRequest) {

    }

    @Override
    public void rerouteMarketDepthRequestProtoBuf(RerouteMarketDepthRequestProto.RerouteMarketDepthRequest rerouteMarketDepthRequest) {

    }

    @Override
    public void secDefOptParameterProtoBuf(SecDefOptParameterProto.SecDefOptParameter secDefOptParameter) {

    }

    @Override
    public void secDefOptParameterEndProtoBuf(SecDefOptParameterEndProto.SecDefOptParameterEnd secDefOptParameterEnd) {

    }

    @Override
    public void softDollarTiersProtoBuf(SoftDollarTiersProto.SoftDollarTiers softDollarTiers) {

    }

    @Override
    public void familyCodesProtoBuf(FamilyCodesProto.FamilyCodes familyCodes) {

    }

    @Override
    public void symbolSamplesProtoBuf(SymbolSamplesProto.SymbolSamples symbolSamples) {

    }

    @Override
    public void smartComponentsProtoBuf(SmartComponentsProto.SmartComponents smartComponents) {

    }

    @Override
    public void marketRuleProtoBuf(MarketRuleProto.MarketRule marketRule) {

    }

    @Override
    public void userInfoProtoBuf(UserInfoProto.UserInfo userInfo) {

    }

    @Override
    public void nextValidIdProtoBuf(NextValidIdProto.NextValidId nextValidId) {

    }

    @Override
    public void currentTimeProtoBuf(CurrentTimeProto.CurrentTime currentTime) {

    }

    @Override
    public void currentTimeInMillisProtoBuf(CurrentTimeInMillisProto.CurrentTimeInMillis currentTimeInMillis) {

    }

    @Override
    public void verifyMessageApiProtoBuf(VerifyMessageApiProto.VerifyMessageApi verifyMessageApi) {

    }

    @Override
    public void verifyCompletedProtoBuf(VerifyCompletedProto.VerifyCompleted verifyCompleted) {

    }

    @Override
    public void displayGroupListProtoBuf(DisplayGroupListProto.DisplayGroupList displayGroupList) {

    }

    @Override
    public void displayGroupUpdatedProtoBuf(DisplayGroupUpdatedProto.DisplayGroupUpdated displayGroupUpdated) {

    }

    @Override
    public void marketDepthExchangesProtoBuf(MarketDepthExchangesProto.MarketDepthExchanges marketDepthExchanges) {

    }
}
