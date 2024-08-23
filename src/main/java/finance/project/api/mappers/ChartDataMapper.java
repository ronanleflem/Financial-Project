package finance.project.api.mappers;

import finance.project.api.entities.FinancialData;
import finance.project.api.model.ChartDataDTO;
import org.mapstruct.Mapper;

@Mapper
public interface ChartDataMapper {

    ChartDataDTO toDto(FinancialData entity);

    FinancialData toEntity(ChartDataDTO dto);
}