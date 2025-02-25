package finance.project.api.services;

@Service
public class OrderFlowService {

    @Autowired
    private OrderFlowRepository orderFlowRepository;

    /**
     * Récupère les volumes acheteurs par niveau institutionnel.
     */
    public List<Double> getBuyVolumes(String symbol, List<Double> keyLevels) {
        return orderFlowRepository.findBuyVolumesForLevels(symbol, keyLevels);
    }

    /**
     * Récupère les volumes vendeurs par niveau institutionnel.
     */
    public List<Double> getSellVolumes(String symbol, List<Double> keyLevels) {
        return orderFlowRepository.findSellVolumesForLevels(symbol, keyLevels);
    }
}