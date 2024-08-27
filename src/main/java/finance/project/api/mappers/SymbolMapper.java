package finance.project.api.mappers;

import finance.project.api.entities.Symbol;
import finance.project.api.model.SymbolDTO;
import org.mapstruct.Mapper;

@Mapper
public interface SymbolMapper {

    SymbolDTO toDto(Symbol entity);

    Symbol toEntity(SymbolDTO dto);
}