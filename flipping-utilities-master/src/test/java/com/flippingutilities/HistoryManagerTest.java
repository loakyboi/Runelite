/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.GrandExchangeOfferState;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class HistoryManagerTest
{
	private static HistoryManager historyManager;
	private static Instant baseTime = Instant.now();

	public OfferInfo offer(boolean isBuy, int currentQuantityInTrade, int price, Instant time, int slot, GrandExchangeOfferState state, int totalQuantityInTrade, int quantitySinceLastOffer) {
		return new OfferInfo(isBuy, 0, currentQuantityInTrade, price, time, slot, state, 0, 0, totalQuantityInTrade, quantitySinceLastOffer, true, true);
	}

	@Before
	public void setUp()
	{
		List<OfferInfo> offers = new ArrayList<>();

		//overall bought 24+3+20=47
		//overall sold 7 + 3 + 30 = 40
		//5gp profit each
		offers.add(offer(true, 7, 100, baseTime.minus(40, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING,24,0));
		offers.add(offer(true, 13, 100, baseTime.minus(30, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING,24, 0));
		offers.add(offer(true, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT,24, 0));

		offers.add(offer(false, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD,7, 0));
		offers.add(offer(false, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.SELLING,5, 0));
		offers.add(offer(false, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL,5, 0));

		offers.add(offer(true, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT,3, 0));
		offers.add(offer(true, 10, 100, baseTime.minus(9, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BUYING,20, 0));
		offers.add(offer(true, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT,20, 0));

		offers.add(offer(false, 10, 105, baseTime.minus(6, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING,30, 0));
		offers.add(offer(false, 20, 105, baseTime.minus(5, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SELLING,30, 0));
		offers.add(offer(false, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD,30, 0));

		historyManager = new HistoryManager();
		for (OfferInfo offer : offers)
		{
			historyManager.updateHistory(offer);
		}
	}

	/**
	 * Tests that updating the history manager standardizes the offers correctly, truncates them appropriately, and
	 * manages state such as ge properties correctly.
	 */
	@Test
	public void historyManagerCorrectlyUpdatedTest()
	{
		List<OfferInfo> recordedOffers = new ArrayList<>();
		recordedOffers.add(new OfferInfo(true, 0, 24, 100, baseTime.minus(20, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BOUGHT, 0, 0, 24, 24, true, true));
		recordedOffers.add(new OfferInfo(false, 0, 7, 105, baseTime.minus(15, ChronoUnit.MINUTES), 3, GrandExchangeOfferState.SOLD, 0, 0, 7, 7, true, true));
		recordedOffers.add(new OfferInfo(false, 0, 3, 105, baseTime.minus(12, ChronoUnit.MINUTES), 4, GrandExchangeOfferState.CANCELLED_SELL, 0, 0, 5, 3, true, true));
		recordedOffers.add(new OfferInfo(true, 0, 3, 100, baseTime.minus(10, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 3, 3, true, true));
		recordedOffers.add(new OfferInfo(true, 0, 20, 100, baseTime.minus(7, ChronoUnit.MINUTES), 2, GrandExchangeOfferState.BOUGHT, 0, 0, 20, 20, true, true));
		recordedOffers.add(new OfferInfo(false, 0, 30, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 0, 0, 30, 30, true, true));


		assertEquals(recordedOffers, historyManager.getStandardizedOffers());

		assertEquals(47, historyManager.getItemsBoughtThisLimitWindow());

		//first buy was 40 mins before baseTime, so ge refresh should be at after 3 hours and 20 minutes which is 200 minutes
		assertEquals(baseTime.plus(200, ChronoUnit.MINUTES), historyManager.getNextGeLimitRefresh());
	}

	@Test
	public void getProfitCorrectnessTest()
	{
		List<OfferInfo> tradesList;
		tradesList = historyManager.getIntervalsHistory(baseTime.minus(1, ChronoUnit.HOURS));
		assertEquals(200, historyManager.currentProfit(tradesList));

		//sell 5 more of the item
		historyManager.updateHistory(offer(false, 5, 105, baseTime.minus(4, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.SOLD, 5, 0));

		tradesList = historyManager.getIntervalsHistory(baseTime.minus(1, ChronoUnit.HOURS));
		assertEquals(225, historyManager.currentProfit(tradesList)); //47 buys and 45 sells, so looks for 45 items and profit is 5 gp ea.

		//when no trades are present given the interval
		tradesList = historyManager.getIntervalsHistory(baseTime);
		assertEquals(0, historyManager.currentProfit(tradesList));
	}

	@Test
	public void gePropertiesCorrectnessTest()
	{
		HistoryManager historyManager = new HistoryManager();

		OfferInfo offer1 = offer(true, 7, 100, baseTime.minus(4, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BUYING, 10, 0);

		//buy 7 of an item 4 hours ago
		historyManager.updateHistory(offer1);
		assertEquals(7, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 3 of that item 3 hours ago, so the amount you bought before the ge limit has refreshed is now 10
		OfferInfo offer2 = offer(true, 10, 100, baseTime.minus(3, ChronoUnit.HOURS), 1, GrandExchangeOfferState.BOUGHT, 10, 0);
		historyManager.updateHistory(offer2);
		assertEquals(10, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer1.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());

		//buy another 1 of that item, but 1 minute in the future, so more than 4 hours from the first purchase of the item. By this time, the ge limit has reset
		//so the amount you bought after the last ge refresh is 1.
		OfferInfo offer3 = offer(true, 1, 100, baseTime.plus(1, ChronoUnit.MINUTES), 1, GrandExchangeOfferState.BUYING, 2, 0);
		historyManager.updateHistory(offer3);
		assertEquals(1, historyManager.getItemsBoughtThisLimitWindow());
		assertEquals(offer3.getTime().plus(4, ChronoUnit.HOURS), historyManager.getNextGeLimitRefresh());
	}

	@Test
	public void testGetFlips()
	{
		//in setup we defined a bunch of offers. The offers signify the following: The user bought 24 of that item, then
		//sold 7 of that item, then sold 3 of that item, then bought 3 of that item, then bought 20 of that item, then
		//sold 30 of that item. So, get flips should return flips that represent all of those complete transaction.
		ArrayList<Flip> generatedFlips;

		ArrayList<Flip> flips = new ArrayList<>();
		flips.add(new Flip(100, 105, 30, baseTime.minus(4, ChronoUnit.MINUTES), false));
		flips.add(new Flip(100, 105, 3, baseTime.minus(12, ChronoUnit.MINUTES), false));
		flips.add(new Flip(100, 105, 7, baseTime.minus(15, ChronoUnit.MINUTES), false));
		generatedFlips = historyManager.getFlips(baseTime.minus(1, ChronoUnit.HOURS));

		assertEquals(generatedFlips, flips);

		//now lets add some margin checks in there!!!!!!!

		OfferInfo marginBuy = new OfferInfo(true, 0, 1, 105, baseTime.minus(3, ChronoUnit.MINUTES),
			1, GrandExchangeOfferState.BOUGHT, 0, 0, 1, 0, true, true);
		OfferInfo marginSell = new OfferInfo(false, 0, 1, 100, baseTime.minus(3, ChronoUnit.MINUTES),
			1, GrandExchangeOfferState.SOLD, 0, 0, 1, 0, true, true);

		historyManager.updateHistory(marginBuy);
		historyManager.updateHistory(marginSell);

		generatedFlips = historyManager.getFlips(baseTime.minus(1, ChronoUnit.HOURS));

		//add the flip generated by the margin check
		flips.add(0, new Flip(105, 100, 1, baseTime.minus(3, ChronoUnit.MINUTES), marginSell.isMarginCheck()));
		assertEquals(generatedFlips, flips);
	}

	@Test
	public void offersCorrectlyTruncatedTest() {
		HistoryManager historyManager = new HistoryManager();

		ArrayList<OfferInfo> someStandardizedOffers = new ArrayList<>();

		ArrayList<OfferInfo> truncatedOffers = new ArrayList<>();

		//no complete offers, should not be truncated
		someStandardizedOffers.add(offer(true, 10, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 10));
		someStandardizedOffers.add(offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1, 5));
		someStandardizedOffers.add(offer(true, 30, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 20));

		truncatedOffers.add(offer(true, 10, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 10));
		truncatedOffers.add(offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1, 5));
		truncatedOffers.add(offer(true, 30, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 20));

		historyManager.truncateOffers(someStandardizedOffers);
		assertEquals(someStandardizedOffers, truncatedOffers);

		//now lets add a completed offer for slot 1.
		someStandardizedOffers.add(offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50, 1));

		//rebuild the truncated offers list
		truncatedOffers.clear();
		truncatedOffers.add(offer(true, 5, 100, baseTime, 2, GrandExchangeOfferState.BUYING, 1, 5));
		truncatedOffers.add(offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50, 50));

		historyManager.truncateOffers(someStandardizedOffers);
		assertEquals(someStandardizedOffers, truncatedOffers);

		//lets add a complete offer for slot 3, this offer has no previous offers in that same trade, so truncate shouldn't even run and the truncated offer should
		//just mirror the current standardized offer list.
		someStandardizedOffers.add(offer(false, 20, 100, baseTime, 3, GrandExchangeOfferState.SOLD, 20, 20));

		truncatedOffers.add(offer(false, 20, 100, baseTime, 3, GrandExchangeOfferState.SOLD, 20, 20));
		//to show the lack of truncation, we will compare the lists before and after the call to truncateOffers

		assertEquals(someStandardizedOffers, truncatedOffers);
		historyManager.truncateOffers(someStandardizedOffers);
		assertEquals(someStandardizedOffers, truncatedOffers);
	}

	@Test
	public void offersAreCorrectlyStandardizedTest() {
		HistoryManager historyManager = new HistoryManager();

		ArrayList<OfferInfo> someUnStandardizedOffers = new ArrayList<>();
		ArrayList<OfferInfo> standardizedOffers = new ArrayList<>();

		//bunch of buy offers from diff slots
		someUnStandardizedOffers.add(offer(true, 10, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 0));
		someUnStandardizedOffers.add(offer(true, 25, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 0));
		someUnStandardizedOffers.add(offer(true, 15, 100, baseTime, 3, GrandExchangeOfferState.BUYING, 20, 0));
		someUnStandardizedOffers.add(offer(true, 20, 100, baseTime, 4, GrandExchangeOfferState.BUYING, 500, 0));
		someUnStandardizedOffers.add(offer(true, 500, 100, baseTime, 4, GrandExchangeOfferState.BOUGHT, 500, 0));
		someUnStandardizedOffers.add(offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50, 0));


		standardizedOffers.add(offer(true, 10, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 10));
		standardizedOffers.add(offer(true, 25, 100, baseTime, 1, GrandExchangeOfferState.BUYING, 50, 15));
		standardizedOffers.add(offer(true, 15, 100, baseTime, 3, GrandExchangeOfferState.BUYING, 20, 15));
		standardizedOffers.add(offer(true, 20, 100, baseTime, 4, GrandExchangeOfferState.BUYING, 500, 20));
		standardizedOffers.add(offer(true, 500, 100, baseTime, 4, GrandExchangeOfferState.BOUGHT, 500, 480));
		standardizedOffers.add(offer(true, 50, 100, baseTime, 1, GrandExchangeOfferState.BOUGHT, 50, 25));
		for (OfferInfo offer: someUnStandardizedOffers) {
			historyManager.storeStandardizedOffer(offer);
		}

		assertEquals(historyManager.getStandardizedOffers(), standardizedOffers);
	}

}
