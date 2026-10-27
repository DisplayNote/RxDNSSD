/*
 * Copyright (C) 2026 DisplayNote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.druk.rxdnssd;

import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.NSClass;
import com.github.druk.dnssd.NSType;
import com.github.druk.dnssd.TXTRecord;

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/** Adds A/AAAA/TXT answers to the service being resolved and emits it after each answer. */
public class RxQueryListenerTest {

    private static final byte[] IPV4 = {(byte) 192, (byte) 168, 1, 10};

    private TestSubscriber<BonjourService> subscriber;
    private BonjourService.Builder builder;
    private DNSSDService query;

    @Before
    public void setUp() {
        subscriber = new TestSubscriber<>();
        builder = new BonjourService.Builder(0, 1, "Living Room", "_airplay._tcp.", "local.");
        query = mock(DNSSDService.class);
    }

    private BonjourService lastEmitted() {
        List<BonjourService> values = subscriber.getOnNextEvents();
        return values.get(values.size() - 1);
    }

    private void answer(RxQueryListener listener, int rrtype, byte[] rdata) {
        listener.queryAnswered(query, 0, 1, "tv.local.", rrtype, NSClass.IN, rdata, 120);
    }

    @Test
    public void aRecordAddsAnIpv4AddressAndKeepsTheStreamOpen() throws UnknownHostException {
        answer(new RxQueryListener(subscriber, builder, false), NSType.A, IPV4);

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
        subscriber.assertNotCompleted();
        assertEquals(InetAddress.getByAddress(IPV4), lastEmitted().getInet4Address());
    }

    @Test
    public void aaaaRecordAddsAnIpv6AddressAndCompletesWhenCompletable() {
        byte[] ipv6 = new byte[16];
        ipv6[0] = (byte) 0xfe;
        ipv6[1] = (byte) 0x80;
        ipv6[15] = 1;

        answer(new RxQueryListener(subscriber, builder, true), NSType.AAAA, ipv6);

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
        assertNotNull(lastEmitted().getInet6Address());
    }

    @Test
    public void answersAccumulateOnTheSameService() {
        RxQueryListener listener = new RxQueryListener(subscriber, builder, false);
        TXTRecord txt = new TXTRecord();
        txt.set("txtvers", "1");

        answer(listener, NSType.A, IPV4);
        answer(listener, NSType.TXT, txt.getRawBytes());

        subscriber.assertValueCount(2);
        assertNotNull(lastEmitted().getInet4Address());
        assertEquals("1", lastEmitted().getTxtRecords().get("txtvers"));
    }

    // Pins current behaviour: onError is followed by onNext (and onCompleted), which breaks the
    // Observable contract. Suspected bug; see docs/runbooks/testing.md.
    @Test
    public void malformedAddressReportsAnErrorAndStillEmits() {
        answer(new RxQueryListener(subscriber, builder, true), NSType.A, new byte[]{1, 2, 3});

        subscriber.assertError(UnknownHostException.class);
        subscriber.assertValueCount(1);
        assertNull(lastEmitted().getInet4Address());
        subscriber.assertCompleted();
    }

    @Test
    public void unsupportedRecordTypeReportsAnErrorAndStillEmits() {
        answer(new RxQueryListener(subscriber, builder, false), NSType.SRV, new byte[0]);

        subscriber.assertError(Exception.class);
        assertEquals("Unsupported type of record: " + NSType.SRV,
                subscriber.getOnErrorEvents().get(0).getMessage());
        subscriber.assertValueCount(1);
    }

    @Test
    public void failureIsEmittedAsAnErrorWithTheCode() {
        new RxQueryListener(subscriber, builder, false).operationFailed(query, -65537);

        subscriber.assertError(RuntimeException.class);
        assertEquals("DNSSD queryRecord error: -65537", subscriber.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void nothingIsEmittedOnceUnsubscribed() {
        subscriber.unsubscribe();
        RxQueryListener listener = new RxQueryListener(subscriber, builder, true);

        answer(listener, NSType.A, IPV4);
        listener.operationFailed(query, -65537);

        subscriber.assertNoValues();
        subscriber.assertNoErrors();
        subscriber.assertNotCompleted();
    }
}
