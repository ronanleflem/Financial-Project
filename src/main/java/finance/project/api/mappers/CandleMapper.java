package finance.project.api.mappers;

import finance.project.api.entities.Candle;
import finance.project.api.model.CandleDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CandleMapper {

    CandleDTO toDto(Candle entity);

    Candle toEntity(CandleDTO dto);
}