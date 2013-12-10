package org.runningdinner.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class RunningDinnerService {

	public BuildTeamsResult buildTeams(final RunningDinnerConfig runningDinnerConfig, final List<TeamMember> teamMembers)
			throws NoPossibleRunningDinnerException {

		int teamSize = runningDinnerConfig.getTeamSize();
		int numParticipants = teamMembers.size();

		if (teamSize >= numParticipants) {
			throw new NoPossibleRunningDinnerException("There must be more participants than a team's size");
		}

		BuildTeamsResult result = new BuildTeamsResult();

		int numTeams = numParticipants / teamSize;

		List<TeamMember> teamMembersToAssign = teamMembers;
		int teamOffset = numParticipants % teamSize;
		if (teamOffset > 0) {
			teamMembersToAssign = teamMembers.subList(0, teamMembers.size() - teamOffset); // TODO Noch -1 oder? Ne stimmt so glaube ich
																							// sogar!
			List<TeamMember> notAssignedMembers = new ArrayList<TeamMember>(teamMembers.subList(teamMembers.size() - 1 - teamOffset,
					teamMembers.size()));
			result.setNotAssignedMembers(notAssignedMembers);
		}

		List<Team> regularTeams = buildRegularTeams(runningDinnerConfig, teamMembersToAssign, numTeams);

		return null;
	}

	private List<Team> buildRegularTeams(final RunningDinnerConfig runningDinnerConfig, final List<TeamMember> teamMembersToAssign,
			final int numTeamsToBuild) {

		List<Team> result = new ArrayList<Team>(numTeamsToBuild);

		int teamSize = runningDinnerConfig.getTeamSize();
		int numNeededSeats = teamSize * runningDinnerConfig.getMealClasses().size();

		Collections.shuffle(teamMembersToAssign); // Sort list randomly!

		Queue<TeamMember> categoryOneList = new ArrayDeque<TeamMember>();
		Queue<TeamMember> categoryTwoList = new ArrayDeque<TeamMember>();
		Queue<TeamMember> uncategeorizedList = new ArrayDeque<TeamMember>();

		if (runningDinnerConfig.isForceEqualDistributedCapacityTeams()) {
			// Distribute team-members based on whether they have enough seats or not:
			for (TeamMember teamMember : teamMembersToAssign) {
				int numSeats = teamMember.getNumSeats();
				if (numSeats != TeamMember.UNDEFINED_SEATS && numSeats >= numNeededSeats) {
					// Enough space
					categoryOneList.offer(teamMember);
				}
				else if (numSeats == TeamMember.UNDEFINED_SEATS) {
					// We don't know...
					uncategeorizedList.offer(teamMember);
				}
				else {
					// Not enough space
					categoryTwoList.offer(teamMember);
				}
			}

			distributeEqually(categoryOneList, uncategeorizedList, categoryTwoList);
			uncategeorizedList.clear();
		}
		else {
			// Equally distribute all team-members over the two category-lists:
			distributeEqually(categoryOneList, teamMembersToAssign, categoryTwoList);
		}

		// Build teams based upon the previously created category-queues:
		for (int i = 0; i < numTeamsToBuild; i++) {

			Set<TeamMember> teamMembers = new HashSet<TeamMember>();

			Queue<TeamMember> currentQueueToPoll = categoryOneList;

			// Try to equally take elements from both category-queues to equally distribute the needed members into one team:
			for (int j = 0; j < teamSize; j++) {

				TeamMember teamMember = currentQueueToPoll.poll();

				if (teamMember != null) {
					teamMembers.add(teamMember);
				}

				// Swap polling queue:
				if (currentQueueToPoll == categoryOneList) {
					currentQueueToPoll = categoryTwoList;
				}
				else {
					currentQueueToPoll = categoryOneList;
				}

				// If previous queue could not return a member try it now with the
				if (teamMember == null) {
					teamMember = currentQueueToPoll.poll();
					teamMembers.add(teamMember);
				}
			}

			Team team = new Team(i + 1);
			team.setTeamMembers(teamMembers);
			result.add(team);
		}

		return result;
	}

	private <T> void distributeEqually(final Collection<T> left, final Collection<T> middle, final Collection<T> right) {
		for (T m : middle) {
			if (left.size() < right.size()) {
				left.add(m);
			}
			else {
				right.add(m);
			}
		}
	}

	public static class BuildTeamsResult {

		private List<Team> regularTeams;

		private List<TeamMember> notAssignedMembers;

		public List<Team> getRegularTeams() {
			return regularTeams;
		}

		void setRegularTeams(List<Team> regularTeams) {
			this.regularTeams = regularTeams;
		}

		public List<TeamMember> getNotAssignedMembers() {
			return notAssignedMembers;
		}

		void setNotAssignedMembers(List<TeamMember> notAssignedMembers) {
			this.notAssignedMembers = notAssignedMembers;
		}

	}
}
