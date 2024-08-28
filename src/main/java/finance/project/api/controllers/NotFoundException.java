package finance.project.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception personnalisée {@code NotFoundException} utilisée pour signaler qu'une ressource demandée n'a pas été trouvée.
 * <p>
 * Cette exception est annotée avec {@link ResponseStatus} pour indiquer que lorsqu'elle est lancée, elle entraîne une réponse HTTP
 * avec le statut {@code 404 Not Found} et un message de raison "Value Not Found". Elle est dérivée de {@link RuntimeException} et
 * fournit plusieurs constructeurs pour la création d'instances avec des messages, des causes ou les deux.
 * </p>
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Value Not Found")
public class NotFoundException extends RuntimeException{
    /**
     * Constructeur sans argument pour {@code NotFoundException}.
     * <p>
     * Utilisé pour créer une instance de {@code NotFoundException} sans message ni cause.
     * </p>
     */
    public NotFoundException() {
    }

    /**
     * Constructeur avec message pour {@code NotFoundException}.
     * <p>
     * Utilisé pour créer une instance de {@code NotFoundException} avec un message descriptif.
     * </p>
     *
     * @param message le message de l'exception
     */
    public NotFoundException(String message) {
        super(message);
    }
}