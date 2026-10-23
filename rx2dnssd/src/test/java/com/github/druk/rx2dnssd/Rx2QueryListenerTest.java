package com.github.druk.rx2dnssd;

import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.NSClass;
import com.github.druk.dnssd.NSType;
import com.github.druk.dnssd.TXTRecord;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import io.reactivex.FlowableEmitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Adds A/AAAA/TXT answers to the service being resolved and emits it after each answer. */
public class Rx2QueryListenerTest {

    private static final byte[] IPV4 = {(byte) 192, (byte) 168, 1, 10};

    private FlowableEmitter<BonjourService> emitter;
    private BonjourService.Builder builder;
    private DNSSDService query;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        emitter = mock(FlowableEmitter.class);
        builder = new BonjourService.Builder(0, 1, "Living Room", "_airplay._tcp.", "local.");
        query = mock(DNSSDService.class);
    }

    private BonjourService lastEmitted() {
        ArgumentCaptor<BonjourService> captor = ArgumentCaptor.forClass(BonjourService.class);
        verify(emitter, atLeastOnce()).onNext(captor.capture());
        return captor.getValue();
    }

    private void answer(Rx2QueryListener listener, int rrtype, byte[] rdata) {
        listener.queryAnswered(query, 0, 1, "tv.local.", rrtype, NSClass.IN, rdata, 120);
    }

    @Test
    public void aRecordAddsAnIpv4AddressAndKeepsTheStreamOpen() throws UnknownHostException {
        answer(new Rx2QueryListener(emitter, builder, false), NSType.A, IPV4);

        assertEquals(InetAddress.getByAddress(IPV4), lastEmitted().getInet4Address());
        verify(emitter, never()).onComplete();
    }

    @Test
    public void aaaaRecordAddsAnIpv6AddressAndCompletesWhenCompletable() {
        byte[] ipv6 = new byte[16];
        ipv6[0] = (byte) 0xfe;
        ipv6[1] = (byte) 0x80;
        ipv6[15] = 1;

        answer(new Rx2QueryListener(emitter, builder, true), NSType.AAAA, ipv6);

        Inet6Address address = lastEmitted().getInet6Address();
        assertEquals(16, address.getAddress().length);
        InOrder order = inOrder(emitter);
        order.verify(emitter).onNext(any(BonjourService.class));
        order.verify(emitter).onComplete();
    }

    @Test
    public void answersAccumulateOnTheSameService() {
        Rx2QueryListener listener = new Rx2QueryListener(emitter, builder, false);
        TXTRecord txt = new TXTRecord();
        txt.set("txtvers", "1");

        answer(listener, NSType.A, IPV4);
        answer(listener, NSType.TXT, txt.getRawBytes());

        BonjourService service = lastEmitted();
        assertTrue(service.getInet4Address() instanceof Inet4Address);
        assertEquals("1", service.getTxtRecords().get("txtvers"));
        verify(emitter, times(2)).onNext(any(BonjourService.class));
    }

    // Pins current behaviour: the error is reported with tryOnError, then the unchanged service
    // is still emitted (and completed). Suspected bug; see docs/runbooks/testing.md.
    @Test
    public void malformedAddressReportsAnErrorAndStillEmits() {
        answer(new Rx2QueryListener(emitter, builder, true), NSType.A, new byte[]{1, 2, 3});

        verify(emitter).tryOnError(any(UnknownHostException.class));
        assertNull(lastEmitted().getInet4Address());
        verify(emitter).onComplete();
    }

    // Same as above for a record type the listener does not handle.
    @Test
    public void unsupportedRecordTypeReportsAnErrorAndStillEmits() {
        answer(new Rx2QueryListener(emitter, builder, false), NSType.SRV, new byte[0]);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(emitter).tryOnError(captor.capture());
        assertEquals("Unsupported type of record: " + NSType.SRV, captor.getValue().getMessage());
        verify(emitter).onNext(any(BonjourService.class));
    }

    @Test
    public void failureIsEmittedAsAnErrorWithTheCode() {
        new Rx2QueryListener(emitter, builder, false).operationFailed(query, -65537);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(emitter).onError(captor.capture());
        assertEquals("DNSSD queryRecord error: -65537", captor.getValue().getMessage());
    }

    @Test
    public void nothingIsEmittedOnceCancelled() {
        when(emitter.isCancelled()).thenReturn(true);
        Rx2QueryListener listener = new Rx2QueryListener(emitter, builder, true);

        answer(listener, NSType.A, IPV4);
        listener.operationFailed(query, -65537);

        verify(emitter, times(2)).isCancelled();
        verifyNoMoreInteractions(emitter);
    }
}
