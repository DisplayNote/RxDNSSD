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

import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;

import org.junit.Before;
import org.junit.Test;

import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/** Turns register callbacks from dnssd into stream events. */
public class RxRegisterListenerTest {

    private TestSubscriber<BonjourService> subscriber;
    private RxRegisterListener listener;

    @Before
    public void setUp() {
        subscriber = new TestSubscriber<>();
        listener = new RxRegisterListener(subscriber);
    }

    @Test
    public void registeredServiceIsEmittedWithTheNameItGot() {
        listener.serviceRegistered(mock(DNSSDRegistration.class), 2, "Living Room (2)", "_airplay._tcp.", "local.");

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
        subscriber.assertNotCompleted();
        BonjourService service = subscriber.getOnNextEvents().get(0);
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

        subscriber.assertError(RuntimeException.class);
        assertEquals("DNSSD browse error: -65548", subscriber.getOnErrorEvents().get(0).getMessage());
    }

    @Test
    public void nothingIsEmittedOnceUnsubscribed() {
        subscriber.unsubscribe();

        listener.serviceRegistered(mock(DNSSDRegistration.class), 0, "a", "_b._tcp.", "local.");
        listener.operationFailed(mock(DNSSDService.class), -65537);

        subscriber.assertNoValues();
        subscriber.assertNoErrors();
    }
}
