/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General private License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General private License for more
 * details.
 *
 * You should have received a copy of the GNU General private License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.instancemanager.event_engine.events;

import com.l2jserver.gameserver.instancemanager.event_engine.AbstractEvent;
import com.l2jserver.gameserver.instancemanager.event_engine.Configuration;
import com.l2jserver.gameserver.instancemanager.event_engine.container.NpcContainer;
import com.l2jserver.gameserver.instancemanager.event_engine.io.Out;
import com.l2jserver.gameserver.instancemanager.event_engine.model.EventNpc;
import com.l2jserver.gameserver.instancemanager.event_engine.model.EventPlayer;
import com.l2jserver.gameserver.instancemanager.event_engine.model.SingleEventStatus;

public class Mutant extends AbstractEvent
{
	public static boolean enabled = true;
        
        private EventNpc spawn;
	
	private class Core implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				switch (eventState)
				{
					case START:
						divideIntoTeams(1);
						teleportToTeamPos();
						preparePlayers();
                                                invulPlayers();
                                                spawn = NpcContainer.getInstance().createNpc(-87791, -153292, -9179, 555, instanceId);                                                
                                                msgToAll("Take your buffs. 30 sec left.");
                                                htmlToAll("data/html/event/Mutant.htm");
                                                Thread.sleep(30000);
						forceSitAll();
                                                uninvulPlayers();
                                                msgToAll("The event starts in 5 seconds.");
						setStatus(EventState.FIGHT);
						schedule(10000);
						break;
					
					case FIGHT:
						forceStandAll();
                                                spawn.unspawn();
						transformMutant(getRandomPlayer());
						setStatus(EventState.END);
						
						clock.start();
						
						break;
					
					case END:
						clock.stop();
						untransformMutant();
						EventPlayer winner = getPlayerWithMaxScore();
						giveReward(winner);
						setStatus(EventState.INACTIVE);
						announce("Congratulation! " + winner.getName() + " won the event with " + winner.getScore() + " kills!");
						eventEnded();
						break;
					default:
						break;
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				announce("Error! Event ended.");
				eventEnded();
			}
		}
	}
	
	private enum EventState
	{
		START,
		FIGHT,
		END,
		INACTIVE
	}
	
	public EventState eventState;
	
	private final Core task;
	
	private EventPlayer mutant;
	
	@SuppressWarnings("synthetic-access")
	public Mutant(Integer containerId)
	{
		super(containerId);
		eventId = 13;
		createNewTeam(1, "All", Configuration.getInstance().getColor(getId(), "All"), Configuration.getInstance().getPosition(getId(), "All", 1));
		task = new Core();
		clock = new EventClock(Configuration.getInstance().getInt(getId(), "matchTime"));
	}
	
	@Override
	public void endEvent()
	{
		setStatus(EventState.END);
		clock.stop();
	}
	
	@Override
	public String getScorebar()
	{
		return "Max: " + getPlayerWithMaxScore().getScore() + "  Time: " + clock.getTimeInString() + "";
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jserver.gameserver.eventengine.AbstractEvent#onClockZero()
	 */
	@Override
	public void onClockZero()
	{
		setStatus(EventState.END);
		schedule(1);
		
	}
	
	@Override
	public void onDie(EventPlayer victim, EventPlayer killer)
	{
		super.onDie(victim, killer);
		addToResurrector(victim);
	}
	
	@Override
	public void onKill(EventPlayer victim, EventPlayer killer)
	{
		super.onKill(victim, killer);
		if (killer.getStatus() == 1)
		{
			killer.increaseScore();
		}
		if ((killer.getStatus() == 0) && (victim.getStatus() == 1))
		{
			transformMutant(killer);
		}
	}
	
	@Override
	public void onLogout(EventPlayer player)
	{
		super.onLogout(player);
		
		if (mutant.equals(player))
		{
			transformMutant(getRandomPlayer());
		}
	}
	
	@Override
	public void schedule(int time)
	{
		Out.tpmScheduleGeneral(task, time);
	}
	
	public void setStatus(EventState s)
	{
		eventState = s;
	}
	
	@Override
	public void start()
	{
		setStatus(EventState.START);
		schedule(1);
	}
	
	public void transformMutant(EventPlayer player)
	{
		player.setNameColor(255, 0, 0);
            switch (player.getOwner().getClassId().getId()) {  
                case 89:
                case 88:
                case 113:
                case 114:
                case 117:
                case 118:
                case 131:
                case 132:
                case 133:
                    player.transform(253); //warrior
                    break;
                case 93:
                case 92:
                case 101:
                case 102:
                case 108:
                case 109:
                case 134:
                    player.transform(254); //Rouge
                    break;
                case 110:
                case 94:
                case 95:
                case 103:
                    player.transform(256); //Wizard
                    break;
                case 98:
                case 100:
                case 107:
                case 115:
                case 116:
                case 136:    
                    player.transform(257); //Enchanter
                    break;    
                case 105:
                case 112:
                case 97:
                    player.transform(255); //Healer
                    break;
                case 90:
                case 91:
                case 99:
                case 106:    
                    player.transform(252); //Knight
                    break;
                case 96:
                case 104:
                case 111:
                    player.transform(258); //Summoner
                    break;
                default:
                    player.transform(253);
                    break;
            }
		player.addSkill(Configuration.getInstance().getInt(getId(), "mutantBuffId"), 1);
                player.removeSkill(3499, 1);
		player.setStatus(1);
		untransformMutant();
		player.broadcastUserInfo();
		mutant = player;
		
	}
	
	public void untransformMutant()
	{
		if (mutant != null)
		{
			mutant.setNameColor(Configuration.getInstance().getColor(getId(), "All")[0], Configuration.getInstance().getColor(getId(), "All")[1], Configuration.getInstance().getColor(getId(), "All")[2]);
			mutant.untransform();
			mutant.removeSkill(Configuration.getInstance().getInt(getId(), "mutantBuffId"), 1);
			mutant.setStatus(0);
			mutant.broadcastUserInfo();
			mutant = null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jserver.gameserver.eventengine.AbstractEvent#createStatus()
	 */
	@Override
	public void createStatus()
	{
		status = new SingleEventStatus(containerId);
		
	}
	
}
