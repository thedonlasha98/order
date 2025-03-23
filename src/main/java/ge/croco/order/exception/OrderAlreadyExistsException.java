package ge.croco.order.exception;

public class OrderAlreadyExistsException extends RuntimeException {
  public OrderAlreadyExistsException(String orderId) {
    super("Order with id " + orderId + " already exists!");
  }
}
