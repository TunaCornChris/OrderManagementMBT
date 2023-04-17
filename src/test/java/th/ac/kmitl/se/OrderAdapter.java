package th.ac.kmitl.se;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

// Update the filename of the saved file of your model here.
@Model(file  = "model.json")
public class OrderAdapter extends ExecutionContext {
    // The following method add some delay between each step
    // so that we can see the progress in GraphWalker player.
    public static int delay = 0;
    @AfterElement
    public void afterEachStep() {
        try
        {
            Thread.sleep(delay);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    OrderDB orderDB;
    ProductDB productDB;
    PaymentService paymentService;
    ShippingService shippingService;
    Address mockAddress;
    Card card;
    Order order;
    @BeforeExecution
    public void setUp() {
        // Add the code here to be executed before
        // GraphWalk starts traversing the model.
        orderDB = mock(OrderDB.class);
        productDB = mock(ProductDB.class);
        paymentService = mock(PaymentService.class);
        shippingService = mock(ShippingService.class);
        order = Mockito.spy(new Order(orderDB, productDB, paymentService, shippingService));
        card = new Card("120", "Chris", 12, 2023);
        mockAddress = new Address("home", "test", "dokmai", "prawet", "bangkok", "10250");
    }

    @Edge()
    public void reset() {
        System.out.println("Edge reset");
        orderDB = mock(OrderDB.class);
        productDB = mock(ProductDB.class);
        paymentService = mock(PaymentService.class);
        shippingService = mock(ShippingService.class);
        order = Mockito.spy(new Order(orderDB, productDB, paymentService, shippingService));
        mockAddress = new Address("home", "test", "dokmai", "prawet", "bangkok", "10250");
        card = new Card("120", "Chris", 12, 2023);
    }

    @Edge()
    public void place() {
        System.out.println("Edge place");
        when(orderDB.getOrderID()).thenReturn(1);

        order.place("John", "Apple Watch", 2, mockAddress);
        assertEquals(Order.Status.PLACED, order.getStatus());
    }

    @Edge()
    public void cancel() {
        System.out.println("Edge cancel");
        order.cancel();
        assertEquals(Order.Status.CANCELED, order.getStatus());
    }

    @Edge()
    public void pay() {
        System.out.println("Edge pay");
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
    }

    @Edge()
    public void retryPay() {
        System.out.println("Retry Pay");
        order.pay(card);
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
    }

    @Edge()
    public void paySuccess() {
        System.out.println("Edge paySuccess");
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess("success");
        assertEquals(Order.Status.PAID, order.getStatus());
        assertEquals(order.paymentConfirmCode, "success");
    }

    @Edge()
    public void payError() {
        System.out.println("Edge payError");
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).pay(any(Card.class), anyFloat(), callbackCaptor.capture());
        callbackCaptor.getValue().onError("error");
        assertEquals(Order.Status.PAYMENT_ERROR, order.getStatus());
    }

    @Edge()
    public void ship() {
        System.out.println("Edge ship");
        when(shippingService.ship(mockAddress, 700.0F)).thenReturn("0001");
        order.ship();
        assertEquals("0001", order.trackingCode);
        assertEquals(Order.Status.SHIPPED, order.getStatus());
    }

    @Edge()
    public void refundSuccess() {
        System.out.println("Edge refundSuccess");
        order.refund();
        //refund from payment service
        ArgumentCaptor<PaymentCallback> callbackCaptorRefund = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).refund(any(), callbackCaptorRefund.capture());
        callbackCaptorRefund.getValue().onSuccess("success");
        assertEquals(Order.Status.REFUNDED, order.getStatus());
    }

    @Edge()
    public void refundError() {
        System.out.println("Edge refundError");
        order.refund();
        assertEquals(Order.Status.AWAIT_REFUND, order.getStatus());
        //refund from payment service
        ArgumentCaptor<PaymentCallback> callbackCaptorRefund = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).refund(any(), callbackCaptorRefund.capture());
        callbackCaptorRefund.getValue().onError("error");
        assertEquals(Order.Status.REFUND_ERROR, order.getStatus());
    }
}