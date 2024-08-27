package finance.project.api.mappers;

import finance.project.api.entities.Symbol;
import finance.project.api.model.SymbolDTO;
import org.mapstruct.Mapper;

// ComponentModel spring : MapStruct génère des beans Spring pour que les mappers soient injectés automatiquement
@Mapper(componentModel = "spring")
public interface SymbolMapper {

    SymbolDTO toDto(Symbol entity);

    Symbol toEntity(SymbolDTO dto);
}