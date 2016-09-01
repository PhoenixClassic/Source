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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.ClassListData;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.SpecialCamera;
import com.l2jserver.gameserver.network.serverpackets.TutorialCloseHtml;
import com.l2jserver.gameserver.network.serverpackets.TutorialShowHtml;
import com.l2jserver.gameserver.network.serverpackets.TutorialShowQuestionMark;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.util.StringUtil;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2MerchantInstance
{
	/**
	 * @param objectId
	 * @param template
	 */
	public L2ClassMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2ClassMasterInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/classmaster/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("1stClass"))
		{
			showHtmlMenu(player, getObjectId(), 1);
		}
		else if (command.startsWith("2ndClass"))
		{
			showHtmlMenu(player, getObjectId(), 2);
		}
		else if (command.startsWith("3rdClass"))
		{
			showHtmlMenu(player, getObjectId(), 3);
		}
		else if (command.startsWith("change_class"))
		{
			int val = Integer.parseInt(command.substring(13));
			
			if (checkAndChangeClass(player, val))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/ok.htm");
				html.replace("%name%", ClassListData.getInstance().getClass(val).getClientCode());
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				player.setNoble(true);
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nobleok.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else if (command.startsWith("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/noclanleader.htm");
				player.sendPacket(html);
			}
			else if (player.getClan().getLevel() >= 5)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/noclanlevel.htm");
				player.sendPacket(html);
			}
			else
			{
				player.getClan().changeLevel(5);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static final void onTutorialLink(L2PcInstance player, String request)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (request == null) || !request.startsWith("CO"))
		{
			return;
		}
		
		if (!player.getFloodProtectors().getServerBypass().tryPerformAction("changeclass"))
		{
			return;
		}
		
		try
		{
			int val = Integer.parseInt(request.substring(2));
			checkAndChangeClass(player, val);
		}
		catch (NumberFormatException e)
		{
		}
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	public static final void onTutorialQuestionMark(L2PcInstance player, int number)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (number != 1001))
		{
			return;
		}
		
		showTutorialHtml(player);
	}
	
	public static final void showQuestionMark(L2PcInstance player)
	{
		if (!Config.ALTERNATE_CLASS_MASTER)
		{
			return;
		}
		
		final ClassId classId = player.getClassId();
		if (getMinLevel(classId.level()) > player.getLevel())
		{
			return;
		}
		
		if (!Config.CLASS_MASTER_SETTINGS.isAllowed(classId.level() + 1))
		{
			return;
		}
		
		player.sendPacket(new TutorialShowQuestionMark(1001));
	}
	
	private static final void showHtmlMenu(L2PcInstance player, int objectId, int level)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		
		if (!Config.ALLOW_CLASS_MASTERS)
		{
			html.setFile(player.getHtmlPrefix(), "data/html/classmaster/disabled.htm");
		}
		else if (!Config.CLASS_MASTER_SETTINGS.isAllowed(level))
		{
			int jobLevel = player.getClassId().level();
			final StringBuilder sb = new StringBuilder(100);
			sb.append("<html><body>");
			switch (jobLevel)
			{
				case 0:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(1))
					{
						sb.append("Come back here when you reached level 20 to change your class.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
					{
						sb.append("Come back after your first occupation change.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 1:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
					{
						sb.append("Come back here when you reached level 40 to change your class.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 2:
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back here when you reached level 76 to change your class.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 3:
					sb.append("There is no class change available for you anymore.<br>");
					break;
			}
			sb.append("</body></html>");
			html.setHtml(sb.toString());
		}
		else
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() >= level)
			{
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nomore.htm");
			}
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if ((player.getLevel() >= minLevel) || Config.ALLOW_ENTIRE_TREE)
				{
					final StringBuilder menu = new StringBuilder(100);
					for (ClassId cid : ClassId.values())
					{
						if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2))
						{
							continue;
						}
						if (validateClassId(currentClassId, cid) && (cid.level() == level))
						{
							StringUtil.append(menu, "<a action=\"bypass -h npc_%objectId%_change_class ", String.valueOf(cid.getId()), "\">", ClassListData.getInstance().getClass(cid).getClientCode(), "</a><br>");
						}
					}
					
					if (menu.length() > 0)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/template.htm");
						html.replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClientCode());
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(minLevel));
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nomore.htm");
					}
				}
			}
		}
		
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}
	
	private static final void showTutorialHtml(L2PcInstance player)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE)
		{
			return;
		}
		
		String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/classmaster/tutorialtemplate.htm");
		msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(currentClassId).getEscapedClientCode());
		
		final StringBuilder menu = new StringBuilder(100);
		for (ClassId cid : ClassId.values())
		{
			if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2))
			{
				continue;
			}
			if (validateClassId(currentClassId, cid))
			{
				StringUtil.append(menu, "<a action=\"link CO", String.valueOf(cid.getId()), "\">", ClassListData.getInstance().getClass(cid).getEscapedClientCode(), "</a><br>");
			}
		}
		
		msg = msg.replaceAll("%menu%", menu.toString());
		msg = msg.replace("%req_items%", getRequiredItems(currentClassId.level() + 1));
		player.sendPacket(new TutorialShowHtml(msg));
	}
	
	private static final boolean checkAndChangeClass(L2PcInstance player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE)
		{
			return false;
		}
		
		if (!validateClassId(currentClassId, val))
		{
			return false;
		}
		
		int newJobLevel = currentClassId.level() + 1;
		
		// Weight/Inventory check
		if (!Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty() && !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return false;
		}
		
		// check if player have all required items for class transfer
		for (int _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keySet())
		{
			int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
			if (player.getInventory().getInventoryItemCount(_itemId, -1) < _count)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return false;
			}
		}
		
		// get all required items for class transfer
		for (int _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keySet())
		{
			int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
			if (!player.destroyItemByItemId("ClassMaster", _itemId, _count, player, true))
			{
				return false;
			}
		}
		
		// reward player with items
        for (int _itemId22 : Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).keySet()) {
                    int _count = Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).get(_itemId22);
            player.addItem("ClassMaster", _itemId22, (long)_count, (L2Object)player, true);
        }
        Vector<Integer> itemids = new Vector<Integer>();
        switch (val) {
            case 1: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(2546);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 4: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(2499);
                itemids.add(2493);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 19: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(2499);
                itemids.add(2493);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 32: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(2499);
                itemids.add(2493);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 45: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(70);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 54: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(159);
                itemids.add(2493);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 56: {
                itemids.add(352);
                itemids.add(2378);
                itemids.add(2411);
                itemids.add(2425);
                itemids.add(2449);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(159);
                itemids.add(2493);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 7: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(225);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 22: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(225);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 35: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(225);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 125: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(9225);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 126: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(9225);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 11: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 15: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 26: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 29: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 39: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 42: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 50: {
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(189);
                player.addItem("ClassMaster", 3948, 3000, (L2Object)player, true);
                break;
            }
            case 47: {
                itemids.add(395);
                itemids.add(417);
                itemids.add(2424);
                itemids.add(2448);
                itemids.add(2411);
                itemids.add(850);
                itemids.add(850);
                itemids.add(881);
                itemids.add(881);
                itemids.add(913);
                itemids.add(262);
                player.addItem("ClassMaster", 1463, 3000, (L2Object)player, true);
                break;
            }
            case 5: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(135);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 6: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(135);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 20: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(135);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 21: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(135);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 33: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(135);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 2: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(2599);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 34: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(2599);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 3: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(298);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 46: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(5286);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 55: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(2503);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 57: {
                itemids.add(356);
                itemids.add(2414);
                itemids.add(2438);
                itemids.add(2462);
                itemids.add(2497);
                itemids.add(2503);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 8: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(228);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 23: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(228);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 36: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(228);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 9: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(286);
                player.addItem("ClassMaster", 1342, 2000, (L2Object)player, true);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 24: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(286);
                player.addItem("ClassMaster", 1342, 2000, (L2Object)player, true);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 37: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(286);
                player.addItem("ClassMaster", 1342, 2000, (L2Object)player, true);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 48: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(266);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 127: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(9296);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 128: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(9292);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 129: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(9292);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 130: {
                itemids.add(398);
                itemids.add(418);
                itemids.add(2431);
                itemids.add(2455);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(9288);
                player.addItem("ClassMaster", 9634, 2000, (L2Object)player, true);
                player.addItem("ClassMaster", 1464, 6000, (L2Object)player, true);
                break;
            }
            case 12: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 13: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 14: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 16: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 17: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 27: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 28: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 30: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 40: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 41: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 43: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 51: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
                break;
            }
            case 52: {
                itemids.add(439);
                itemids.add(471);
                itemids.add(2430);
                itemids.add(2454);
                itemids.add(2414);
                itemids.add(886);
                itemids.add(886);
                itemids.add(855);
                itemids.add(855);
                itemids.add(119);
                itemids.add(206);
                player.addItem("ClassMaster", 3949, 6000, (L2Object)player, true);
            }
        }
        Iterator i$ = itemids.iterator();
        while (i$.hasNext()) {
            int id = (Integer)i$.next();
            L2ItemInstance itm = player.addItem("ClassMaster", id, 1, (L2Object)player, true);
            player.getInventory().equipItem(itm);
        }
        player.sendPacket((L2GameServerPacket)new SpecialCamera(player.getObjectId(), 3, 0, 0, 300, 5500, 360, 0, 1, 0));
        player.broadcastPacket((L2GameServerPacket)new SocialAction(player.getObjectId(), 3));
		
		player.setClassId(val);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.broadcastUserInfo();
		
		if (Config.CLASS_MASTER_SETTINGS.isAllowed(player.getClassId().level() + 1) && Config.ALTERNATE_CLASS_MASTER && (((player.getClassId().level() == 1) && (player.getLevel() >= 40)) || ((player.getClassId().level() == 2) && (player.getLevel() >= 76))))
		{
			showQuestionMark(player);
		}
		
		return true;
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static final int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param val new class index
	 * @return
	 */
	private static final boolean validateClassId(ClassId oldCID, int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.getClassId(val));
		}
		catch (Exception e)
		{
			// possible ArrayOutOfBoundsException
		}
		return false;
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
	private static final boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		if (oldCID.equals(newCID.getParent()))
		{
			return true;
		}
		
		if (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
		{
			return true;
		}
		
		return false;
	}
	
	private static String getRequiredItems(int level)
	{
		if ((Config.CLASS_MASTER_SETTINGS.getRequireItems(level) == null) || Config.CLASS_MASTER_SETTINGS.getRequireItems(level).isEmpty())
		{
			return "<tr><td>none</td></tr>";
		}
		StringBuilder sb = new StringBuilder();
		for (int _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(level).keySet())
		{
			int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(level).get(_itemId);
			sb.append("<tr><td><font color=\"LEVEL\">" + _count + "</font></td><td>" + ItemTable.getInstance().getTemplate(_itemId).getName() + "</td></tr>");
		}
		return sb.toString();
	}
}
