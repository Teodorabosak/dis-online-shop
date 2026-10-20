package rs.ftn.dis.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.ftn.dis.inventory.domain.InsufficientStockException;
import rs.ftn.dis.inventory.service.StockNotFoundException;

/**
 * Pretvara izuzetke iz poslovne logike u HTTP odgovore:
 * 404 - nema evidencije, 409 - nema dovoljno robe ili istovremena izmena, 400 - los zahtev.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(StockNotFoundException.class)
    public ProblemDetail handleNotFound(StockNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Zalihe je istovremeno menjao drugi zahtev, pokusajte ponovo");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
