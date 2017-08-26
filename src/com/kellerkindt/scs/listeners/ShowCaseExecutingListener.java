    /*
 * ShowCaseStandalone - A Minecraft-Bukkit-API Shop Plugin
 * Copyright (C) 2016-08-16 22:43 +02 kellerkindt (Michael Watzko) <copyright at kellerkindt.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
    package com.kellerkindt.scs.listeners;

    import com.kellerkindt.scs.ReflectionHandler;
    import com.kellerkindt.scs.ShowCaseStandalone;
    import com.kellerkindt.scs.events.ShowCaseCreateEvent;
    import com.kellerkindt.scs.events.ShowCaseDeleteEvent;
    import com.kellerkindt.scs.events.ShowCaseEvent;
    import com.kellerkindt.scs.events.ShowCaseInfoEvent;
    import com.kellerkindt.scs.events.ShowCaseInteractEvent;
    import com.kellerkindt.scs.events.ShowCaseItemAddEvent;
    import com.kellerkindt.scs.events.ShowCaseItemRemoveEvent;
    import com.kellerkindt.scs.events.ShowCaseLimitEvent;
    import com.kellerkindt.scs.events.ShowCaseMemberAddEvent;
    import com.kellerkindt.scs.events.ShowCaseMemberRemoveEvent;
    import com.kellerkindt.scs.events.ShowCaseOwnerSetEvent;
    import com.kellerkindt.scs.events.ShowCasePlayerBuyEvent;
    import com.kellerkindt.scs.events.ShowCasePlayerExchangeEvent;
    import com.kellerkindt.scs.events.ShowCasePlayerSellEvent;
    import com.kellerkindt.scs.events.ShowCasePriceSetEvent;
    import com.kellerkindt.scs.events.ShowCaseRemoveEvent;
    import com.kellerkindt.scs.interfaces.ShowCaseListener;
    import com.kellerkindt.scs.internals.NamedUUID;
    import com.kellerkindt.scs.shops.BuyShop;
    import com.kellerkindt.scs.shops.DisplayShop;
    import com.kellerkindt.scs.shops.ExchangeShop;
    import com.kellerkindt.scs.shops.SellShop;
    import com.kellerkindt.scs.shops.Shop;
    import com.kellerkindt.scs.utilities.ItemStackUtilities;
    import com.kellerkindt.scs.utilities.MaterialNames;
    import com.kellerkindt.scs.utilities.Term;
    import net.md_5.bungee.api.chat.BaseComponent;
    import net.md_5.bungee.api.chat.HoverEvent;
    import net.md_5.bungee.api.chat.TextComponent;
    import org.bukkit.ChatColor;
    import org.bukkit.enchantments.Enchantment;
    import org.bukkit.entity.Player;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.EventPriority;
    import org.bukkit.inventory.ItemStack;

    import java.lang.reflect.Method;

    /**
     * This class executes the request behind the event
     * if it wasn't cancelled before
     * IMPORTANT: This listener doesn't check anything, it will
     * execute any event that is incoming! (see {@link ShowCaseVerifyingListener}
     * Also ignores a lot of issues (like full inventory)
     * @author kellerkindt <michael at kellerkindt.com>
     */
    public class ShowCaseExecutingListener implements ShowCaseListener {

        private ShowCaseStandalone scs;

        Method asNMSCopy; //CraftItemStack#asNMSCopy(ItemStack);
        Method saveNMSItemStack; //n.m.s.ItemStack#save(compound);
        Class<?> NBTTagCompoundClazz; //n.m.s.NBTTagCompound;

        public ShowCaseExecutingListener (ShowCaseStandalone scs) {
            this.scs    = scs;
            try
            {
                asNMSCopy = ReflectionHandler.getMethod("CraftItemStack", ReflectionHandler.PackageType.CRAFTBUKKIT_INVENTORY, "asNMSCopy", ItemStack.class);
                NBTTagCompoundClazz = ReflectionHandler.PackageType.MINECRAFT_SERVER.getClass("NBTTagCompound");
                saveNMSItemStack = ReflectionHandler.getMethod("ItemStack", ReflectionHandler.PackageType.MINECRAFT_SERVER, "save", NBTTagCompoundClazz);
            }
            catch (Exception e)
            {
                scs.getLogger().warning("Ur reflection sux lol, no items4u");
                e.printStackTrace();
            }
        }

        /**
         * Is called, if a player requested information about a Shop
         * @param scie    ShowCaseInfoEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)    // monitor is to check the outcome (http://wiki.bukkit.org/Event_API_Reference)
        public void onShowCaseInfoEvent (ShowCaseInfoEvent scie) {

            for (String line : scie.getShop().getDescription()) {
                scs.sendMessage(
                        scie.getPlayer(),
                        line
                );
            }

            //Send player the item information in a json hover message
            //It's generally not fun diving into NMSland...
            ItemStack itemStack = scie.getShop().getItemStack();
            String json;
            try
            {
                Object nmsItemStack = asNMSCopy.invoke(null, itemStack); //CraftItemStack#asNMSCopy(itemStack); //nms version of the ItemStack
                Object nbtTagCompound = NBTTagCompoundClazz.newInstance(); //new NBTTagCompoundClazz(); //get a new NBTTagCompound, which will contain the nmsItemStack.
                nbtTagCompound = saveNMSItemStack.invoke(nmsItemStack, nbtTagCompound); //nmsItemStack#save(nbtTagCompound); //saves nmsItemStack into our new NBTTagCompound
                json = nbtTagCompound.toString();
            }
            catch (Exception e)
            {
                return;
            }

            BaseComponent[] hoverEventComponents = new BaseComponent[]
                    {
                            new TextComponent(json)
                    };
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents);
            TextComponent text = new TextComponent(ChatColor.AQUA + "[Hover for item info]");
            text.setHoverEvent(hover);
            scie.getPlayer().sendMessage(text);
        }

        /**
         * Is called, if a player requested an interaction between him and the shop
         * @param scie    ShowCaseInteractEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseInteractEvent (ShowCaseInteractEvent scie) {
            if (scie.isConsumed()) {
                return; // nothing to do
            }

            Player    player    = scie.getPlayer();
            Shop    shop    = scie.getShop();

            ShowCaseEvent    event    = null;

            // just to be really really sure ^^
            if (shop == null) {
                return;
            }

            // quantity is one by default, if the player is sneaking it's its set sneak amount
            int quantity = player.isSneaking() ? scs.getPlayerSessionHandler().getSession(player).getUnitSize() : 1;

            // buy / sell / exchange
            if (shop instanceof BuyShop) {
                event     = new ShowCasePlayerSellEvent(player, (BuyShop)shop, quantity);

            } else if (shop instanceof SellShop) {
                event     = new ShowCasePlayerBuyEvent(player, (SellShop)shop, quantity);

            } else if (shop instanceof ExchangeShop) {
                event     = new ShowCasePlayerExchangeEvent(player, (ExchangeShop)shop, quantity);

            } else if (shop instanceof DisplayShop) {
                event = new ShowCaseInfoEvent(player, shop);

            }



            if (event != null) {
                // perform the event
                scs.callShowCaseEvent(event, scie.getPlayer());
            }
        }

        /**
         * Is called, if items should be added to a shop
         * @param sciae    ShowCaseItemAddEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseItemAddEvent (ShowCaseItemAddEvent sciae) {
            Shop            shop    = sciae.getShop();
            ExchangeShop    shopEx    = shop instanceof ExchangeShop ? ((ExchangeShop)shop) : null;
            int                amount    = sciae.getAmount();

            if (shop.getItemStack().isSimilar(sciae.getItemStack())) {
                // add the amount
                shop.setAmount(shop.getAmount() + amount);

                // set the message
                sciae.setMsgSuccessfully(Term.INVENTORY_UPDATE.get(""+amount, ""+shop.getAmount()));

            } else if (shopEx != null && shopEx.getExchangeItemStack().isSimilar(sciae.getItemStack())) {

                // add the amount
                shopEx.setExchangeAmount(shopEx.getExchangeAmount() + amount);

                // set the message
                sciae.setMsgSuccessfully(Term.INVENTORY_UPDATE.get(""+amount, ""+shopEx.getExchangeAmount()));
            }
        }

        /**
         * Is called, if a member should be added to a shop
         * @param scmae    ShowCaseMemberAddEvent with needed information about the shop and the member to add
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseMemberAddEvent (ShowCaseMemberAddEvent scmae) {
            Shop      shop   = scmae.getShop();
            NamedUUID member = scmae.getMember();

            shop.addMember(member);
            scmae.setMsgSuccessfully(Term.MESSAGE_SUCCESSFULL_ADDED_MEMBER.get());
        }

        /**
         * Is called, if a new shop should be created
         * @param scce    ShowCaseCreateEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseCreateEvent (ShowCaseCreateEvent scce) {
            if (scs.getConfiguration().isDebuggingShopCreation()) {
                scs.getLogger().info("Entered ShowCaseExecutingListener::onShowCaseCreateEvent");
            }

            // get the shop to add
            Shop   shop = scce.getShop();
            double cost = scs.getCreatePrice(shop.getClass());

            if (scs.getConfiguration().isDebuggingShopCreation()) {
                scs.getLogger().info("Adding shop="+shop+" to ShopHandler");
            }

            // add the shop
            scs.getShopHandler().addShop(shop);
            scs.getShopHandler().show   (shop);

            if (scs.getConfiguration().isDebuggingShopCreation()) {
                scs.getLogger().info("Going to take creation cost="+cost);
            }

            // remove the money
            scs.getBalanceHandler().sub(scce.getPlayer(), cost);
            scce.setMsgSuccessfully(Term.MESSAGE_SUCCESSFULL_CREATED.get());

            if (scs.getConfiguration().isDebuggingShopCreation()) {
                scs.getLogger().info("Leaving ShowCaseExecutingListener::onShowCaseCreateEvent");
            }
        }

        /**
         * Is called, if a shop should be deleted
         * @param scde    ShowCaseDeleteEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseDeleteEvent (ShowCaseDeleteEvent scde) {
            scs.getShopHandler().hide        (scde.getShop());
            scs.getShopHandler().removeShop    (scde.getShop());
            scde.setMsgSuccessfully(Term.MESSAGE_SUCCESSFULL_DESTROYED.get());
        }

        /**
         * Is called, if items should be removed from a shop
         * @param scire    ShowCaseItemRemoveEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseItemRemoveEvent (ShowCaseItemRemoveEvent scire) {

            Shop            shop    = scire.getShop();
            ExchangeShop    shopEx    = shop instanceof ExchangeShop ? ((ExchangeShop)shop) : null;
            int                amount    = scire.getAmount();

            if (shop.getItemStack().isSimilar(scire.getItemStack())) {

                // add the amount
                shop.setAmount(shop.getAmount() - amount);

                // set the message
                scire.setMsgSuccessfully(Term.INVENTORY_UPDATE.get(""+amount, ""+shop.getAmount()));

            } else if (shopEx != null && shopEx.getExchangeItemStack().isSimilar(scire.getItemStack())) {

                // add the amount
                shopEx.setExchangeAmount(shopEx.getExchangeAmount() - amount);

                // set the message
                scire.setMsgSuccessfully(Term.INVENTORY_UPDATE.get(""+amount, ""+shopEx.getExchangeAmount()));
            }
        }


        /**
         * Is called, if a new limit should set for the shop
         * @param scle ShowCaseLimitEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseLimitEvent (ShowCaseLimitEvent scle) {
            if (scle.getShop() instanceof BuyShop) {
                BuyShop shop = (BuyShop)scle.getShop();

                shop.setMaxAmount(scle.getLimit());
                scle.setMsgSuccessfully(Term.MESSAGE_BUY_LIMIT.get(""+shop.getMaxAmount()));
            }
        }

        /**
         * Is called, if the shop should be removed
         * @param scre    ShowCaseRemoveEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseRemoveEvent (ShowCaseRemoveEvent scre) {
            scs.getShopHandler().hide        (scre.getShop());
            scs.getShopHandler().removeShop    (scre.getShop());
            scre.setMsgSuccessfully(Term.MESSAGE_SUCCESSFULL_REMOVED.get());
        }

        /**
         * Is called, if a member should be removed
         * @param scmre    ShowCaseMemeberRemoveEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseMemberRemoveEvent (ShowCaseMemberRemoveEvent scmre) {
            scmre.getShop().removeMember( scmre.getMember() );
            scmre.setMsgSuccessfully(Term.MESSAGE_SUCCESSFULL_REMOVED_MEMBER.get());
        }

        /**
         * Is called, if the owner of a shop should be set to another one
         * @param scose    ShowCaseOwnerSetEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCaseOwnerSetEvent (ShowCaseOwnerSetEvent scose) {
            scose.getShop().setOwner(scose.getNewOwner());
            scose.setMsgSuccessfully(Term.MESSAGE_SET_OWNER.get(""+scose.getShop().getOwner()));
        }

        /**
         * Is called, if the price of a shop should be set to another one
         * @param scpse ShowCasePriceSetEvent with needed information about the shop and player
         */
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCasePriceSetEvent (ShowCasePriceSetEvent scpse) {
            scpse.getShop().setPrice(scpse.getPrice());
            scpse.setMsgSuccessfully(Term.MESSAGE_SET_PRICE.get(""+scpse.getPrice()));
        }


        /**
         * @see com.kellerkindt.scs.interfaces.ShowCaseListener#onShowCasePlayerBuyEvent(com.kellerkindt.scs.events.ShowCasePlayerBuyEvent)
         */
        @Override
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCasePlayerBuyEvent(ShowCasePlayerBuyEvent scpbe) {

            Shop    shop    = scpbe.getShop();

            int     added = ItemStackUtilities.addToInventory(scpbe.getPlayer().getInventory(), shop.getItemStack(), scpbe.getQuantity());
            double    price = added * shop.getPrice();

            if (!shop.isUnlimited() && scs.getBalanceHandler().exists(shop.getOwner())) {
                // the owner only gets the money if the shop is not unlimited
                scs.getBalanceHandler().add(shop.getOwner(), price);
            }

            // the costumer always has to pay the items
            scs.getBalanceHandler().sub(scpbe.getPlayer(), price);

            // ignore unlimited - later you can see how many were sold ^^
            shop.setAmount(shop.getAmount() - added);

            // contact the owner
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_SELL_OWNER_1.get(shop.getItemStack().getType().toString(), ""+shop.getAmount()));
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_SELL_OWNER_2.get(scpbe.getPlayer().getDisplayName(), ""+scpbe.getQuantity(), ""+price));

            // set successfully message
            scpbe.setMsgSuccessfully(Term.MESSAGE_SELL_COSTUMER.get(MaterialNames.getItemName(shop.getItemStack()), ""+added, ""+price));
        }


        /**
         * @see com.kellerkindt.scs.interfaces.ShowCaseListener#onShowCasePlayerSellEvent(com.kellerkindt.scs.events.ShowCasePlayerSellEvent)
         */
        @Override
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCasePlayerSellEvent(ShowCasePlayerSellEvent scpse) {

            Player        player    = scpse.getPlayer();
            BuyShop     shop     = scpse.getShop();

            int     removed = ItemStackUtilities.removeFromInventory(player.getInventory(), shop.getItemStack(), scpse.getQuantity(), scs.compareItemMeta(shop.getItemStack()));
            double    price    = removed * shop.getPrice();


            if (!shop.isUnlimited() && scs.getBalanceHandler().exists(shop.getOwner())) {
                // the owner only looses its money if it isn't a unlimited shop
                scs.getBalanceHandler().sub(shop.getOwner(), price);
            }

            // the costumer has always to get its money
            scs.getBalanceHandler().add(player, price);


            // add the amount that was removed
            shop.setAmount(shop.getAmount() + removed);

            // contact the owner
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_BUY_OWNER_1.get(shop.getItemStack().getType().toString(), ""+shop.getAmount(), ""+shop.getMaxAmount()));
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_BUY_OWNER_2.get(scpse.getPlayer().getDisplayName(), ""+scpse.getQuantity(), ""+price));

            // set successfully message
            scpse.setMsgSuccessfully(Term.MESSAGE_BUY.get(MaterialNames.getItemName(shop.getItemStack()), ""+removed, ""+price));
        }


        /**
         * @see com.kellerkindt.scs.interfaces.ShowCaseListener#onShowCasePlayerExchangeEvent(com.kellerkindt.scs.events.ShowCasePlayerExchangeEvent)
         */
        @Override
        @EventHandler (ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onShowCasePlayerExchangeEvent(ShowCasePlayerExchangeEvent scpee) {

            // 2013-0X-XX dude, check it ^^
            // 2014-03-02 found and fixed issue xD

            Player            player    = scpee.getPlayer();
            ExchangeShop    shop     = scpee.getShop();
            double            price    = shop.getPrice();

            int     buyAmount        = scpee.getQuantity();
            double     removeAmount    = buyAmount * price;

            // it always gets more expensive
            if (removeAmount > ((int)removeAmount)) {
                removeAmount = ((int)removeAmount) +1;
            }

            // do not divide by 0 or less
            if (removeAmount <= 0) {
                return;
            }

            int removed    = ItemStackUtilities.removeFromInventory(
                    player.getInventory(),
                    shop.getExchangeItemStack(),
                    (int)removeAmount,
                    scs.compareItemMeta(shop.getExchangeItemStack()));
            int toAdd    = (int)(removed / price);

            // add items for that was payed
            ItemStackUtilities.addToInventory(player.getInventory(), shop.getItemStack(), toAdd);

            // remove the items from the shop
            shop.setAmount            ( shop.getAmount()             - toAdd     );
            shop.setExchangeAmount    ( shop.getExchangeAmount()     + removed     );


            // contact the owner
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_SELL_OWNER_1.get(shop.getItemStack().getType().toString(), ""+shop.getAmount()));
            scs.sendTransactionMessageToOwner(shop, Term.MESSAGE_SELL_OWNER_2.get(scpee.getPlayer().getDisplayName(), ""+scpee.getQuantity(), ""+price));

            scpee.setMsgSuccessfully(Term.MESSAGE_SELL_COSTUMER.get(MaterialNames.getItemName(shop.getItemStack()), ""+removed, ""+toAdd));
        }
    }
