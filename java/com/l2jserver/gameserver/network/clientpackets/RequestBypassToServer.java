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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.communitybbs.CommunityBoard;
import com.l2jserver.gameserver.datatables.AdminTable;
import com.l2jserver.gameserver.datatables.ClassListData;
import com.l2jserver.gameserver.handler.AdminCommandHandler;
import com.l2jserver.gameserver.handler.BypassHandler;
import com.l2jserver.gameserver.handler.IAdminCommandHandler;
import com.l2jserver.gameserver.handler.IBypassHandler;
import com.l2jserver.gameserver.handler.IVoicedCommandHandler;
import com.l2jserver.gameserver.handler.VoicedCommandHandler;
import com.l2jserver.gameserver.instancemanager.event_engine.Interface;
import com.l2jserver.gameserver.instancemanager.rank_system.rankpvpsystem.RankPvpSystemPlayerInfo;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Hero;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.RequestShowCommunityBoard;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ConfirmDlg;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.scripting.scriptengine.events.RequestBypassToServerEvent;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.talk.RequestBypassToServerListener;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.L2SkillType;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.util.GMAudit;
import com.l2jserver.gameserver.util.Util;
import java.util.Collection;
import javolution.text.TextBuilder;

/**
 * This class ...
 * @version $Revision: 1.12.4.5 $ $Date: 2005/04/11 10:06:11 $
 */
public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final String _C__23_REQUESTBYPASSTOSERVER = "[C] 23 RequestBypassToServer";
	private static final List<RequestBypassToServerListener> _listeners = new FastList<RequestBypassToServerListener>().shared();
	
	// S
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(_command))
		{
			return;
		}
		
		if (_command.isEmpty())
		{
			_log.info(activeChar.getName() + " send empty requestbypass");
			activeChar.logout();
			return;
		}
		
		try
		{
			if (_command.startsWith("admin_"))
			{
				String command = _command.split(" ")[0];
				
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
				
				if (ach == null)
				{
					if (activeChar.isGM())
					{
						activeChar.sendMessage("The command " + command.substring(6) + " does not exist!");
					}
					_log.warning(activeChar + " requested not registered admin command '" + command + "'");
					return;
				}
				
				if (!AdminTable.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access rights to use this command!");
					_log.warning("Character " + activeChar.getName() + " tried to use admin command " + command + ", without proper access level!");
					return;
				}
				
				if (AdminTable.getInstance().requireConfirm(command))
				{
					activeChar.setAdminConfirmCmd(_command);
					ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1);
					dlg.addString("Are you sure you want execute command " + _command.substring(6) + " ?");
					activeChar.sendPacket(dlg);
				}
				else
				{
					if (Config.GMAUDIT)
					{
						GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));
					}
					
					ach.useAdminCommand(_command, activeChar);
				}
			}
			else if (_command.startsWith(".")) {
				String command = _command.substring(1).split(" ")[0];
				String params = _command.substring(1).split(" ").length > 1 ? _command.substring(1).split(" ")[1] : "";
				IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(command);
				if (vch == null)
				{
					_log.warning(activeChar + " requested not registered admin command '" + command + "'");
					return;
				}
				vch.useVoicedCommand(command, activeChar, params);
			}
			else if (_command.equals("come_here") && activeChar.isGM())
			{
				comeHere(activeChar);
			}
                        
        else if(_command.startsWith("buff")){
           String[] val = _command.split(" ");
            String x = val[1];
            String y = val[2];
            int lvl = Integer.parseInt(y);
            int id = Integer.parseInt(x);
            L2PcInstance target = null;
           
            if(activeChar.getTarget() instanceof L2PcInstance)
            target = (L2PcInstance) activeChar.getTarget();
            
            if(!activeChar.isInsideRadius(target, 150, false, false)){
                activeChar.sendMessage("Out of range!");
                return;
            }
           
            if(target == null)
              return;
            
            if (!target.isSellBuff())
                    return;
            
            if(!target.isInsideZone(ZoneId.PEACE)){
               activeChar.sendMessage("The buffer is not in peaceful zone!");
               return; 
            }
              
           
            if(activeChar.getInventory().getItemByItemId(57) == null || activeChar.getInventory().getItemByItemId(57).getCount() < ((L2PcInstance) activeChar.getTarget()).getBuffPrize()){
              activeChar.sendMessage("Not enough adena!");
              return;
           }
           
       
            try{
            L2Skill s = SkillTable.getInstance().getInfo(id, lvl);
            s.getEffects(activeChar, activeChar);
            activeChar.sendMessage("Buff activated: "+s.getName());
            activeChar.getInventory().destroyItemByItemId("", 57, target.getBuffPrize(), activeChar, null);
            target.getInventory().addItem("", 57, target.getBuffPrize(), target, null);
            TextBuilder tb = new TextBuilder();
            NpcHtmlMessage n = new NpcHtmlMessage(0);
           
            tb.append("<html><body>");
            tb.append("<br><br>");
            tb.append("<center>Hello in my Buff Shop!</center>");
            tb.append("<br><center>All buff price: <font color=LEVEL>"+target.getBuffPrize()+"</font> Adena!</center><br><center><table><tr>");
           
           
               Collection<L2Skill> skills = target.getAllSkills();
            FastList<L2Skill> ba = new FastList<L2Skill>();
           
            for(L2Skill skill : skills){
              if(skill == null)
                  continue;
             
             
              if(skill.getSkillType() == L2SkillType.BUFF && skill.isActive() && skill.getId() != 970 && skill.getId() != 357 && skill.getId() != 1323 && skill.getId() != 327 && skill.getId() != 1325 && skill.getId() != 1326 && skill.getId() != 1327 && skill.getId() != 1540 && skill.getId() != 1533 && skill.getId() != 1412 && skill.getId() != 1411 && skill.getId() !=1532 && skill.getId() != 1540 && skill.getId() !=1520 && skill.getId() !=1521 && skill.getId() !=1522 && skill.getId() != 1506 && skill.getId() != 1507 && skill.getId() != 1543 && skill.getId() !=988 && skill.getId() !=989 && skill.getId() !=1430 && skill.getId() != 1217 && skill.getId() != 1219 && skill.getId() !=1531 && skill.getId() !=1229 && skill.getId() !=1256 && skill.getId() !=1427 && skill.getId() != 123 && skill.getId() !=77 && skill.getId() != 91 && skill.getId() != 110 && skill.getId() != 112 && skill.getId() != 913 && skill.getId() != 230)
                  ba.add(skill);
            }
           
            for(L2Skill p : ba){
                int enc = p.getLevel();
                    if (enc < 100)
                        enc = 0;
                    if (enc > 100 && enc <200)
                            enc = enc-100;
                    if (enc > 200 && enc < 300)
                            enc = enc-200;
                    if (enc > 300)
                            enc = enc-300;
                if (p.getId() == 1517)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill1499\"></td>");
                else if (p.getId() == 1518)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill1502\"></td>");
                else if (p.getId() < 1000)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill0"+p.getId()+"\"></td>");
                else
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill"+p.getId()+"\"></td>");
                tb.append("<td><button value=\""+p.getName()+" +" + enc +"\" action=\"bypass -h buff "+p.getId()+" "+p.getLevel()+"\" width=200 height=32 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
                if (p.getId() == 1517)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill1499\"></td></tr><tr>");
                if (p.getId() == 1518)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill1502\"></td></tr><tr>");
                else if (p.getId() < 1000)
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill0"+p.getId()+"\"></td></tr><tr>");
                else
                    tb.append("<td><img width=32 height=32 src=\"Icon.skill"+p.getId()+"\"></td></tr><tr>");
            }
           
            tb.append("</tr></table></center></body></html>");
           
            n.setHtml(tb.toString());
            activeChar.sendPacket(n);
            }
            catch(Exception e){
              e.printStackTrace();
            }
        }
        else if(_command.startsWith("actr")){
            String l = _command.substring(5);
           
            int p = 0;

            p = Integer.parseInt(l);
           
           
            if(p == 0)
              return;
           
            if(p > 2000000000){
              activeChar.sendMessage("Too expensive!");
              return;
            }
           
              activeChar.setBuffPrize(p);
              activeChar.sitDown();
              activeChar.setSellBuff(true);

              activeChar.setOldTitle(activeChar.getTitle());
              activeChar.setOldNameColor(activeChar.getAppearance().getNameColor());
              /*activeChar.getAppearance().setNameColor(0x55155);*/
              activeChar.getAppearance().setNameColor(99, 22, 11);
              activeChar.setTitle(ClassListData.getInstance().getClass(activeChar.getClassId()).getClassName());
              activeChar.broadcastUserInfo();
              activeChar.broadcastTitleInfo();
        }

                        
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
				{
					id = _command.substring(4, endOfId);
				}
				else
				{
					id = _command.substring(4);
				}
				if (Util.isDigit(id))
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					
					if ((object != null) && object.isNpc() && (endOfId > 0) && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
					{
						((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
				}
				
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else if (_command.startsWith("item_"))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
				{
					id = _command.substring(5, endOfId);
				}
				else
				{
					id = _command.substring(5);
				}
				try
				{
					L2ItemInstance item = activeChar.getInventory().getItemByObjectId(Integer.parseInt(id));
					
					if ((item != null) && (endOfId > 0))
					{
						item.onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
					
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
					_log.log(Level.WARNING, "NFE for command [" + _command + "]", nfe);
				}
			}
			else if (_command.startsWith("phoenix "))
			{
				Interface.bypass(activeChar.getObjectId(), _command.substring(8));
			}
			else if (_command.startsWith("summon_"))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				int endOfId = _command.indexOf('_', 8);
				String id;
				
				if (endOfId > 0)
				{
					id = _command.substring(7, endOfId);
				}
				else
				{
					id = _command.substring(7);
				}
				
				if (Util.isDigit(id))
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					
					if ((object instanceof L2MerchantSummonInstance) && (endOfId > 0) && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
					{
						((L2MerchantSummonInstance) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
				}
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			}
			// Navigate through Manor windows
			else if (_command.startsWith("manor_menu_select"))
			{
				final IBypassHandler manor = BypassHandler.getInstance().getHandler("manor_menu_select");
				if (manor != null)
				{
					manor.useBypass(_command, activeChar, null);
				}
			}
			else if (_command.startsWith("eventmanager"))
			{
				Interface.bypass(activeChar.getObjectId(), _command.substring(13));
			}
			else if (_command.startsWith("_bbs"))
			{
				if (Config.ENABLE_COMMUNITY_BOARD)
				{
					if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), _command)))
					{
						activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
					}
				}
				else
				{
					CommunityBoard.getInstance().handleCommands(getClient(), _command);
				}
			}
			else if (_command.startsWith("bbs"))
			{
				if (Config.ENABLE_COMMUNITY_BOARD)
				{
					if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), _command)))
					{
						activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
					}
				}
				else
				{
					CommunityBoard.getInstance().handleCommands(getClient(), _command);
				}
			}
			else if (_command.startsWith("_mail"))
			{
				if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), "_bbsmail")))
				{
					activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
				}
			}
			else if (_command.startsWith("_friend"))
			{
				if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), "_bbsfriend")))
				{
					activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
				}
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
				{
					return;
				}
				
				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');
				if (idx < 0)
				{
					activeChar.processQuestEvent(p, "");
				}
				else
				{
					activeChar.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
				}
			}
			else if (_command.startsWith("_match"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
				{
					Hero.getInstance().showHeroFights(activeChar, heroclass, heroid, heropage);
				}
			}
			else if (_command.startsWith("_diary"))
			{
				String params = _command.substring(_command.indexOf("?") + 1);
				StringTokenizer st = new StringTokenizer(params, "&");
				int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
				{
					Hero.getInstance().showHeroDiary(activeChar, heroclass, heroid, heropage);
				}
			}
			else if (_command.startsWith("_olympiad?command"))
			{
				int arenaId = Integer.parseInt(_command.split("=")[2]);
				final IBypassHandler handler = BypassHandler.getInstance().getHandler("arenachange");
				if (handler != null)
				{
					handler.useBypass("arenachange " + (arenaId - 1), activeChar, null);
				}
			}
			else if (_command.equals("_rps_equip"))
			{ // for "details" button
				try
				{
					if (activeChar._rankPvpSystemDeathMgr != null)
					{
						if (activeChar._rankPvpSystemDeathMgr.getKiller() != null)
						{
							activeChar._rankPvpSystemDeathMgr.sendVictimResponse();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_command.equals("_rps_info"))
			{ // for "back" button
				try
				{
					if (activeChar._rankPvpSystemDeathMgr != null)
					{
						// required for death manager, shows killer info:
						RankPvpSystemPlayerInfo playerInfo = new RankPvpSystemPlayerInfo();
						if (activeChar._rankPvpSystemDeathMgr.getKiller() != null)
						{
							playerInfo.sendPlayerResponse(activeChar, activeChar._rankPvpSystemDeathMgr.getKiller());
						}
						playerInfo = null;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_command.equals("_rps_reward"))
			{ // for "get reward" button
				try
				{
					if ((activeChar._rankPvpSystemRankPointsReward != null) && (activeChar._rankPvpSystemRankPointsReward.getRankRewardsCount() > 0) && (activeChar._rankPvpSystemRankPointsReward.getPlayer() != null))
					{
						activeChar._rankPvpSystemRankPointsReward.addRankRewardsToInventory();
						activeChar._rankPvpSystemRankPointsReward = null;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				final IBypassHandler handler = BypassHandler.getInstance().getHandler(_command);
				if (handler != null)
				{
					handler.useBypass(_command, activeChar, null);
				}
				else
				{
					_log.log(Level.WARNING, getClient() + " sent not handled RequestBypassToServer: [" + _command + "]");
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, getClient() + " sent bad RequestBypassToServer: \"" + _command + "\"", e);
			if (activeChar.isGM())
			{
				StringBuilder sb = new StringBuilder(200);
				sb.append("<html><body>");
				sb.append("Bypass error: " + e + "<br1>");
				sb.append("Bypass command: " + _command + "<br1>");
				sb.append("StackTrace:<br1>");
				for (StackTraceElement ste : e.getStackTrace())
				{
					sb.append(ste.toString() + "<br1>");
				}
				sb.append("</body></html>");
				// item html
				NpcHtmlMessage msg = new NpcHtmlMessage(0, 12807);
				msg.setHtml(sb.toString());
				msg.disableValidation();
				activeChar.sendPacket(msg);
			}
		}
		
		fireBypassListeners();
	}
	
	/**
	 * @param activeChar
	 */
	private static void comeHere(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if (obj == null)
		{
			return;
		}
		if (obj instanceof L2Npc)
		{
			L2Npc temp = (L2Npc) obj;
			temp.setTarget(activeChar);
			temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 0));
		}
	}
	
	/**
	 * Fires the event when packet arrived.
	 */
	private void fireBypassListeners()
	{
		RequestBypassToServerEvent event = new RequestBypassToServerEvent();
		event.setActiveChar(getActiveChar());
		event.setCommand(_command);
		
		for (RequestBypassToServerListener listener : _listeners)
		{
			listener.onRequestBypassToServer(event);
		}
	}
	
	/**
	 * @param listener
	 */
	public static void addBypassListener(RequestBypassToServerListener listener)
	{
		if (!_listeners.contains(listener))
		{
			_listeners.add(listener);
		}
	}
	
	/**
	 * @param listener
	 */
	public static void removeBypassListener(RequestBypassToServerListener listener)
	{
		if (_listeners.contains(listener))
		{
			_listeners.remove(listener);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__23_REQUESTBYPASSTOSERVER;
	}
}
