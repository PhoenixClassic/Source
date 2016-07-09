/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
//modified by Light & Savay Syntax egy ganaj
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.Config;
import java.util.*;
//import com.l2jserver.gameserver.cache.HtmCache;
//import com.l2jserver.gameserver.instancemanager.event_engine.io.Out;
//import com.l2jserver.gameserver.instancemanager.event_engine.model.ManagerNpcHtml;
import com.l2jserver.gameserver.model.Elementals;
//import com.l2jserver.gameserver.instancemanager.event_engine.io.In;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExAttributeEnchantResult;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;
//import javolution.text.TextBuilder;

public class RequestExEnchantItemAttribute extends L2GameClientPacket
{
        L2ItemInstance item2 = null;
        L2ItemInstance stone2 = null;
	private static final String _C__D0_35_REQUESTEXENCHANTITEMATTRIBUTE = "[C] D0:35 RequestExEnchantItemAttribute";
	//private static final int c = 0;
	private int _objectId;
        public static int k;
        private static Map <Integer,Integer> map = new HashMap<Integer,Integer>();
        private static List<Integer> list = new ArrayList<Integer>();
        
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
        
        public int attridb(int player, int i)
        {
            	//L2PcInstance player = getClient().getActiveChar();
                int l = player;
                //_log.log(Level.WARNING, "attridb" + Integer.toString(l));
                if (map.isEmpty())
                {
                    map.put(l,i);
                    list.add(l);
                    k = i;
                    //_log.log(Level.WARNING, "return1");
                    return k;
                }
                    for(int m=0; m<=list.size(); m++)   
                    {
                        //_log.log(Level.WARNING, "listsize " + Integer.toString(list.size()));
                        if(list.size()!=m && list.get(m)==l)
                        {
                            int log = map.get(list.get(m));
                            //_log.log(Level.WARNING, "oldvalue " + Integer.toString(log));
                            //_log.log(Level.WARNING, "newvalue " + Integer.toString(i));
                            map.put(l,i);
                            k = i;
                            //_log.log(Level.WARNING, "return2");
                            return k;
                        }
                        if(list.size()==m)
                        {
                            map.put(l,i);
                            list.add(l);
                            k = i;
                            //_log.log(Level.WARNING, "return3");
                            return k;
                        }
                    }
           // _log.log(Level.WARNING, "return4");
            k=i;
            return k;
        }
               
	@Override
	protected void runImpl()
	{
            //int n;
                
		L2PcInstance player = getClient().getActiveChar();
                int id = player.getObjectId();
              //  _log.log(Level.WARNING, "clientid" + Integer.toString(id));
                int iddb = map.get(id);
                //_log.log(Level.WARNING, "almagetter" +k); 
                //In a = new In ();
                //k = a.getalma();
		//htmContent = htmReplace(htmContent, "tableId");
                //int k=1;
                if (iddb<0 || iddb>300)
                {
                    return;
                }
                for (int j=0;j<iddb;j++)
                {                
		if (player == null)
		{
			return;
		}

		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.setActiveEnchantAttrItem(null);
			player.sendPacket(SystemMessageId.ELEMENTAL_ENHANCE_CANCELED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.setActiveEnchantAttrItem(null);
			return;
		}
		
		if (player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendPacket(SystemMessageId.CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_PRIVATE_STORE_OR_WORKSHOP);
			player.setActiveEnchantAttrItem(null);
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails)
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.setActiveEnchantAttrItem(null);
			player.sendMessage("Enchanting items is not allowed during a trade.");
			return;
		}

                    //_log.log(Level.WARNING, "alma" +j);            
		L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		L2ItemInstance stone = player.getActiveEnchantAttrItem();

                if (j==0)
                {
                    item2 = item;
                    stone2 = stone;
                    //_log.log(Level.WARNING, "item2=item"); 
                }
                if (j!=0)
                {
                    item = item2;
                    stone = stone2;
                   // _log.log(Level.WARNING, "item=item2"); 
                }
		if ((item == null) || (stone == null))
		{
			player.setActiveEnchantAttrItem(null);
                        //_log.log(Level.WARNING, "return1"); 
			return;
		}
		
		if (!item.isElementable())
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_ENHANCE_REQUIREMENT_NOT_SUFFICIENT);
			player.setActiveEnchantAttrItem(null);
                        //_log.log(Level.WARNING, "return2"); 
			return;
		}
		
		switch (item.getLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.setActiveEnchantAttrItem(null);
                                        //_log.log(Level.WARNING, "return3"); 
					return;
				}
				break;
			}
			default:
			{
				player.setActiveEnchantAttrItem(null);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
                               // _log.log(Level.WARNING, "return4"); 
				return;
			}
		}
		
		int stoneId = stone.getItemId();
		byte elementToAdd = Elementals.getItemElement(stoneId);
		// Armors have the opposite element
		if (item.isArmor())
		{
			elementToAdd = Elementals.getOppositeElement(elementToAdd);
		}
		byte opositeElement = Elementals.getOppositeElement(elementToAdd);
		
		Elementals oldElement = item.getElemental(elementToAdd);
		int elementValue = oldElement == null ? 0 : oldElement.getValue();
		int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		
		if ((item.isWeapon() && (oldElement != null) && (oldElement.getElement() != elementToAdd) && (oldElement.getElement() != -2)) || (item.isArmor() && (item.getElemental(elementToAdd) == null) && (item.getElementals() != null) && (item.getElementals().length >= 3)))
		{
			player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_ALREADY_ADDED);
			player.setActiveEnchantAttrItem(null);
                        //_log.log(Level.WARNING, "return5"); 
			return;
		}
		
		if (item.isArmor() && (item.getElementals() != null))
		{
			// cant add opposite element
			for (Elementals elm : item.getElementals())
			{
				if (elm.getElement() == opositeElement)
				{
					player.setActiveEnchantAttrItem(null);
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to add oposite attribute to item!", Config.DEFAULT_PUNISH);
					//_log.log(Level.WARNING, "return6"); 
                                        return;
				}
			}
		}
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_ENHANCE_CANCELED);
			player.setActiveEnchantAttrItem(null);
                        //_log.log(Level.WARNING, "return7"); 
			return;
		}
		
		if (!player.destroyItem("AttrEnchant", stone, 1, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to attribute enchant with a stone he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantAttrItem(null);
                       // _log.log(Level.WARNING, "return8"); 
			return;
		}
   
                
		boolean success = false;

		switch (Elementals.getItemElemental(stoneId)._type)
		{
			case Stone:
			case Roughore:
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_STONE;
				break;
			case Crystal:
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_CRYSTAL;
				break;
			case Jewel:
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_JEWEL;
				break;
			case Energy:
				success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_ENERGY;
				break;
		}
		if (success)
		{
			byte realElement = item.isArmor() ? opositeElement : elementToAdd;
			
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				if (item.isArmor())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S2_ATTRIBUTE_WAS_SUCCESSFULLY_BESTOWED_ON_S1_RES_TO_S3_INCREASED);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ELEMENTAL_POWER_S2_SUCCESSFULLY_ADDED_TO_S1);
				}
				sm.addItemName(item);
				sm.addElemental(realElement);
				if (item.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			else
			{
				if (item.isArmor())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S3_ATTRIBUTE_BESTOWED_ON_S1_S2_RESISTANCE_TO_S4_INCREASED);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ELEMENTAL_POWER_S3_SUCCESSFULLY_ADDED_TO_S1_S2);
				}
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
				sm.addElemental(realElement);
				if (item.isArmor())
				{
					sm.addElemental(Elementals.getOppositeElement(realElement));
				}
			}
			player.sendPacket(sm);                       
			item.setElementAttr(elementToAdd, newPower);
			if (item.isEquipped())
			{
				item.updateElementAttrBonus(player);
			}
			
			// send packets
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
		else
		{
			player.sendPacket(SystemMessageId.FAILED_ADDING_ELEMENTAL_POWER);
		}
		
		player.sendPacket(new ExAttributeEnchantResult(powerToAdd));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.setActiveEnchantAttrItem(null);
        }
	}
	
	public int getLimit(L2ItemInstance item, int sotneId)
	{
		Elementals.ElementalItems elementItem = Elementals.getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return Elementals.WEAPON_VALUES[elementItem._type._maxLevel];
		}
		return Elementals.ARMOR_VALUES[elementItem._type._maxLevel];
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, L2ItemInstance item)
	{
		if (Elementals.getItemElement(stoneId) != Elementals.NONE)
		{
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return Elementals.FIRST_WEAPON_BONUS;
				}
				return Elementals.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return Elementals.ARMOR_BONUS;
			}
		}
		return 0;
	}
	
	@Override
	public String getType()
	{
		return _C__D0_35_REQUESTEXENCHANTITEMATTRIBUTE;
	}
}
