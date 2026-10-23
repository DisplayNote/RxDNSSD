package com.github.druk.rx2dnssd;

import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.reactivex.FlowableEmitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Turns register callbacks from dnssd into stream events. */
public class Rx2RegisterListenerTest {

    private FlowableEmitter<BonjourService> emitter;
    private Rx2RegisterListener listener;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        emitter = mock(FlowableEmitter.class);
        listener = new Rx2RegisterListener(emitter);
    }

    @Test
    public void registeredServiceIsEmittedWithTheNameItGot() {
        listener.serviceRegistered(mock(DNSSDRegistration.class), 2, "Living Room (2)", "_airplay._tcp.", "local.");

        ArgumentCaptor<BonjourService> captor = ArgumentCaptor.forClass(BonjourService.class);
        verify(emitter).onNext(captor.capture());
        BonjourService service = captor.getValue();
        assertEquals(2, service.getFlags());
        assertEquals(0, service.getIfIndex());
        assertEquals("Living Room (2)", service.getServiceName());
        assertEquals("_airplay._tcp.", service.getRegType());
        assertEquals("local.", service.getDomain());
        assertNull(service.getHostname());
    }

    // The message says "browse" for a register failure; pinned as is (docs/runbooks/testing.md).
    @Test
    public void failureIsEmittedAsAnErrorWithTheCode() {
        listener.operationFailed(mock(DNSSDService.class), -65548);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(emitter).onError(captor.capture());
        assertEquals(RuntimeException.class, captor.getValue().getClass());
        assertEquals("DNSSD browse error: -65548", captor.getValue().getMessage());
    }

    @Test
    public void nothingIsEmittedOnceCancelled() {
        when(emitter.isCancelled()).thenReturn(true);

        listener.serviceRegistered(mock(DNSSDRegistration.class), 0, "a", "_b._tcp.", "local.");
        listener.operationFailed(mock(DNSSDService.class), -65537);

        verify(emitter, times(2)).isCancelled();
        verifyNoMoreInteractions(emitter);
    }
}
