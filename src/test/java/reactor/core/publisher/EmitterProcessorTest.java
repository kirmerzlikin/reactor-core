/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.scheduler.Schedulers;

/**
 * @author Stephane Maldini
 */
public class EmitterProcessorTest {

	@Test
	public void testColdIdentityProcessor() throws InterruptedException {
		final int elements = 10;
		CountDownLatch latch = new CountDownLatch(elements + 1);

		Processor<Integer, Integer> processor = EmitterProcessor.create(16);

		Flux.range(1, 10)
		    .subscribe(processor);

		List<Integer> list = new ArrayList<>();

		processor.subscribe(new Subscriber<Integer>() {
			Subscription s;

			@Override
			public void onSubscribe(Subscription s) {
				this.s = s;
				s.request(1);
			}

			@Override
			public void onNext(Integer integer) {
				synchronized (list) {
					list.add(integer);
				}
				latch.countDown();
				if (latch.getCount() > 0) {
					s.request(1);
				}
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}

			@Override
			public void onComplete() {
				System.out.println("completed!");
				latch.countDown();
			}
		});
		//stream.broadcastComplete();

		latch.await(8, TimeUnit.SECONDS);

		long count = latch.getCount();
		org.junit.Assert.assertTrue("Count > 0 : " + count + " (" + list + ")  , Running on " + Schedulers.DEFAULT_POOL_SIZE + " CPU",
				latch.getCount() == 0);

	}

	/*@Test
	public void test100Hot() throws InterruptedException {
		for (int i = 0; i < 10000; i++) {
			testHotIdentityProcessor();
		}
	}
*/
	@Test
	public void testHotIdentityProcessor() throws InterruptedException {
		final int elements = 10000;
		CountDownLatch latch = new CountDownLatch(elements);

		Processor<Integer, Integer> processor = EmitterProcessor.create(1024);

		EmitterProcessor<Integer> stream = EmitterProcessor.create();
		BlockingSink<Integer> session = BlockingSink.create(stream);
		stream.subscribe(processor);

		processor.subscribe(new Subscriber<Integer>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(elements);
			}

			@Override
			public void onNext(Integer integer) {
				latch.countDown();
			}

			@Override
			public void onError(Throwable t) {
				System.out.println("error! " + t);
			}

			@Override
			public void onComplete() {
				System.out.println("completed!");
				//latch.countDown();
			}
		});

		for (int i = 0; i < elements; i++) {
			if (session.submit(i, 1000) == -1) {
			}
		}
		//stream.then();

		latch.await(8, TimeUnit.SECONDS);

		long count = latch.getCount();
		org.junit.Assert.assertTrue("Count > 0 : " + count + "  , Running on " + Schedulers.DEFAULT_POOL_SIZE + " CPU",
				latch.getCount() == 0);

		stream.onComplete();

	}
}
