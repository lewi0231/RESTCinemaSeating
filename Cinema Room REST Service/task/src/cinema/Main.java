package cinema;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}

@RestController
class Controller {
    Tickets tickets = new Tickets();

    @GetMapping("/seats")
    public Tickets getTickets(){
        return tickets;
    }

    @PostMapping("/purchase")
    public PurchasedTicket purchaseTicket(@RequestBody Ticket ticket){;
        return tickets.getTicket(ticket.getRow() , ticket.getColumn() );
    }

    @PostMapping("/return")
    public ReturnedTicketHandler returnTicket(@RequestBody UUIDModel token){
        if (tickets.containsPurchasedTicket(token.getToken())){
            return tickets.returnTicket(token.getToken());
        }
        throw new PurchaseRequestException("Wrong token!");
    }

    @PostMapping("/stats")
    public Stats obtainStatistics(@RequestParam(required = false) String password){

        if (password != null && !password.equals("")){
            return new Stats(tickets.getTotalIncome(), tickets.getNumberOfAvailableSeats(),
                    tickets.getNumberOfPurchasedTickets());
        }
        throw new IncorrectPasswordException("The password is wrong!");
    }
}

class Stats {

    private int currentIncome;
    private int numberOfAvailableSeats;
    private int numberOfPurchasedTickets;

    public Stats(int currentIncome, int numberOfAvailableSeats, int numberOfPurchasedTickets) {
        this.currentIncome = currentIncome;
        this.numberOfAvailableSeats = numberOfAvailableSeats;
        this.numberOfPurchasedTickets = numberOfPurchasedTickets;
    }

    @JsonGetter("current_income")
    public int getCurrentIncome() {
        return currentIncome;
    }

    public void setCurrentIncome(int currentIncome) {
        this.currentIncome = currentIncome;
    }

    @JsonGetter("number_of_available_seats")
    public int getNumberOfAvailableSeats() {
        return numberOfAvailableSeats;
    }

    public void setNumberOfAvailableSeats(int numberOfAvailableSeats) {
        this.numberOfAvailableSeats = numberOfAvailableSeats;
    }

    @JsonGetter("number_of_purchased_tickets")
    public int getNumberOfPurchasedTickets() {
        return numberOfPurchasedTickets;
    }

    public void setNumberOfPurchasedTickets(int numberOfPurchasedTickets) {
        this.numberOfPurchasedTickets = numberOfPurchasedTickets;
    }
}

class UUIDModel{

    UUID token;

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }
}

class Tickets {
    private final int totalRows = 9;
    private final int totalColumns = 9;

    private int totalIncome = 0;
    private final List<Ticket> availableTickets = new ArrayList<>();

    private final ConcurrentHashMap<UUID, PurchasedTicket> purchasedTickets = new ConcurrentHashMap<>();

    public Tickets() {
        for (int i = 1; i <= totalRows; i++) {
            for (int j = 1; j <= totalColumns; j++) {
                int price;
                if (i <= 4){
                    price = 10;
                } else {
                    price = 8;
                }
                availableTickets.add(new Ticket(i, j, price));
            }
        }
    }

    public int getTotalIncome() {
        return totalIncome;
    }

    @JsonIgnore
    public void setTotalIncome(int totalIncome) {
        this.totalIncome = totalIncome;
    }

    public boolean containsPurchasedTicket(UUID token){
        return purchasedTickets.containsKey(token);
    }

    public ReturnedTicketHandler returnTicket(UUID token){
        PurchasedTicket ticket = purchasedTickets.get(token);
        availableTickets.add(ticket.getTicket());
        totalIncome -= ticket.getTicket().getPrice();
        purchasedTickets.remove(token);
        return new ReturnedTicketHandler(ticket.getTicket());
    }

    @JsonGetter("total_rows")
    public int getTotalRows(){
        return totalRows;
    }

    @JsonGetter("total_columns")
    public  int getTotalColumns(){
        return totalColumns;
    }

    @JsonGetter("available_seats")
    public List<Ticket> getAvailableTickets(){
        return availableTickets;
    }

    @JsonIgnore
    public int getNumberOfAvailableSeats(){
        return availableTickets.size();
    }

    @JsonIgnore
    public int getNumberOfPurchasedTickets(){
        return purchasedTickets.size();
    }

    public PurchasedTicket getTicket(int row, int column){
        if (row > totalRows || column > totalColumns ||
        row < 1 || column < 1){
            throw new PurchaseRequestException("The number of a row or a column is out of bounds!");
        } else {
            for (Ticket t : availableTickets){
                if (t.getRow() == row && t.getColumn() == column && !t.isPurchased()){
                    t.setPurchased(true);
                    totalIncome += t.getPrice();
                    availableTickets.remove(t);
                    PurchasedTicket ticket = new PurchasedTicket(t);
                    purchasedTickets.put(ticket.getToken(), ticket);
                    return ticket;
                }
            }
            throw new PurchaseRequestException("The ticket has been already purchased!");
        }
    }
}

class ReturnedTicketHandler{
    private Ticket returned_ticket;

    public ReturnedTicketHandler(Ticket returned_ticket) {
        this.returned_ticket = returned_ticket;
    }

    public Ticket getReturned_ticket() {
        return returned_ticket;
    }

    public void setReturned_ticket(Ticket returned_ticket) {
        this.returned_ticket = returned_ticket;
    }
}


class PurchasedTicket{
    UUID token;
    Ticket ticket;

    public PurchasedTicket(Ticket ticket) {
        this.token = UUID.randomUUID();
        this.ticket = ticket;
    }

    public UUID getToken() {
        return token;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}

class Ticket {
    private int row;
    private int column;
    private int price;
    @JsonIgnore
    private boolean purchased;

    public Ticket(int row, int column, int price){
        this.row = row;
        this.column = column;
        this.price = price;
        this.purchased = false;
    }
    public Ticket(){}

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
}




class PurchaseRequestException extends RuntimeException {
    public PurchaseRequestException(String message) {
        super(message);
    }
}

class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}

@ControllerAdvice
class GeneralExceptionHandler {

    @ExceptionHandler(value = PurchaseRequestException.class)
    public ResponseEntity<Object> handlePurchaseException(PurchaseRequestException e){
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;
        GeneralException exception = new GeneralException(e.getMessage(), badRequest);
        return new ResponseEntity<>(exception, badRequest);
    }

    @ExceptionHandler(value = IncorrectPasswordException.class)
    public ResponseEntity<Object> handleIncorrectPasswordException(IncorrectPasswordException e){
        HttpStatus unauthorized = HttpStatus.UNAUTHORIZED;
        GeneralException exception = new GeneralException(e.getMessage(), unauthorized);
        return new ResponseEntity<>(exception, unauthorized);
    }
}

class GeneralException {
    private String error;
    private HttpStatus httpStatus; 

    public GeneralException(String error, HttpStatus status) {
        this.error = error;
        httpStatus = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
