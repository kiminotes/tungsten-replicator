/**
 * Tungsten Scale-Out Stack
 * Copyright (C) 2013 Continuent Inc.
 * Contact: tungsten@continuent.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * Initial developer(s): Edward Archibald
 * Contributor(s): Robert Hodges
 */

package com.continuent.tungsten.common.cluster.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.continuent.tungsten.common.utils.CLUtils;

/**
 * Encapsulates the logic to determine whether a set of members represents a
 * primary partition that may continue to operate as a cluster. This class also
 * includes logic to determine whether the current GC view is consistent and
 * whether the digest itself is valid.
 */
public class ClusterMembershipDigest
{
    // Base data.
    private String                         name;
    private Vector<String>                 configuredMembers  = new Vector<String>();
    private Vector<String>                 viewMembers        = new Vector<String>();
    private Vector<String>                 witnessMembers     = new Vector<String>();

    // Set consisting of union of all known members.
    private HashMap<String, ClusterMember> quorumSet          = new HashMap<String, ClusterMember>();

    // Witness host definition, if used.
    private HashMap<String, ClusterMember> witnessSet         = new HashMap<String, ClusterMember>();

    // Counters for number of members marked validated and reachable.
    private int                            validated          = 0;
    private int                            reachable          = 0;
    private int                            reachableWitnesses = 0;

    /**
     * Instantiates a digest used to compute whether the member that creates the
     * digest is in a primary group.
     * 
     * @param name Name of this member
     * @param configuredMembers Member names from service configuration
     * @param viewMembers Member names from group communications view
     * @param witnessMember Name of the witness host
     */
    public ClusterMembershipDigest(String name,
            Collection<String> configuredMembers,
            Collection<String> viewMembers, List<String> witnessMembers)
    {
        // Assign values.
        this.name = name;
        if (configuredMembers != null)
        {
            this.configuredMembers.addAll(configuredMembers);
        }
        if (viewMembers != null)
        {
            this.viewMembers.addAll(viewMembers);
        }
        if (witnessMembers != null)
        {
            this.witnessMembers.addAll(witnessMembers);
        }

        // Construct quorum set.
        deriveQuorumSet();
    }

    // Construct the quorum set, which is the union of the configured and view
    // members.
    private void deriveQuorumSet()
    {
        // Add configured members first.
        for (String name : configuredMembers)
        {
            ClusterMember cm = new ClusterMember(name);
            cm.setConfigured(true);
            quorumSet.put(name, cm);
        }

        // Now iterate across the view members and add new member definitions or
        // update existing ones.
        for (String name : viewMembers)
        {
            ClusterMember cm = quorumSet.get(name);
            if (cm == null)
            {
                cm = new ClusterMember(name);
                cm.setInView(true);
                quorumSet.put(name, cm);
            }
            else
            {
                cm.setInView(true);
            }
        }

        // Add the witness hosts if we have any.
        for (String name : witnessMembers)
        {
            ClusterMember witness = new ClusterMember(name);
            witness.setWitness(true);
            witnessSet.put(name, witness);
        }
    }

    /** Return name of current member. */
    public String getName()
    {
        return name;
    }

    /** Return the number of members required to have a simple majority. */
    public int getSimpleMajoritySize()
    {
        return ((quorumSet.size() / 2) + 1);
    }

    /**
     * Sets the validation flag on a member.
     * 
     * @param member Name of the member that was tested
     * @param valid If true member was validated through GC ping
     */
    public void setValidated(String member, boolean valid)
    {
        ClusterMember cm = quorumSet.get(member);
        if (cm != null)
        {
            cm.setValidated(valid);
            if (valid)
                validated++;
        }
    }

    /**
     * Sets the reachability flag on a member.
     * 
     * @param member Name of the member that was tested
     * @param reached If true member was reached with a network ping command
     */
    public void setReachable(String member, boolean reached)
    {
        ClusterMember cm = quorumSet.get(member);
        if (cm != null)
        {
            cm.setReachable(true);
            reachable++;
        }
        else
        {
            ClusterMember witness = witnessSet.get(member);
            if (member.equals(witness.getName()))
            {
                witness.setReachable(true);
                reachableWitnesses++;
            }
        }
    }

    /** Return quorum set members. */
    public List<ClusterMember> getQuorumSetMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(
                quorumSet.size());
        list.addAll(quorumSet.values());
        return list;
    }

    /** Return definitions of the configured members. */
    public List<ClusterMember> getConfiguredSetMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(
                configuredMembers.size());
        for (String name : configuredMembers)
        {
            list.add(quorumSet.get(name));
        }
        return list;
    }

    /** Return definitions of the view members. */
    public List<ClusterMember> getViewSetMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(
                viewMembers.size());
        for (String name : viewMembers)
        {
            list.add(quorumSet.get(name));
        }
        return list;
    }

    /** Return definitions of the witness members. */
    public List<ClusterMember> getWitnessSetMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(
                witnessSet.size());
        for (ClusterMember cm : witnessSet.values())
        {
            list.add(cm);
        }
        return list;
    }

    /** Return the validated members. */
    public List<ClusterMember> getValidatedMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(validated);
        for (ClusterMember cm : quorumSet.values())
        {
            // Validated members must have been checked *and* must have
            // a true value.
            Boolean valid = cm.getValidated();
            if (valid != null && valid)
                list.add(cm);
        }
        return list;
    }

    /** Return the reachable members. */
    public List<ClusterMember> getReachableMembers()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(validated);
        for (ClusterMember cm : quorumSet.values())
        {
            // Reachable members must have been checked *and* must have
            // a true value.
            Boolean reachable = cm.getReachable();
            if (reachable != null && reachable)
                list.add(cm);
        }
        return list;
    }

    /** Return the reachable witnesses. */
    public List<ClusterMember> getReachableWitnesses()
    {
        ArrayList<ClusterMember> list = new ArrayList<ClusterMember>(
                reachableWitnesses);
        for (ClusterMember cm : witnessSet.values())
        {
            // Reachable witnesses must have been checked *and* must have
            // a true value.
            Boolean reachable = cm.getReachable();
            if (reachable != null && reachable)
                list.add(cm);
        }
        return list;
    }

    /** Return member names from the quorum set. */
    public List<String> getQuorumSetNames()
    {
        return clusterMembersToNames(quorumSet.values());
    }

    /** Return validated member names. */
    public List<String> getValidatedMemberNames()
    {
        return clusterMembersToNames(getValidatedMembers());
    }

    /** Return reachable member names. */
    public List<String> getReachableMemberNames()
    {
        return clusterMembersToNames(getReachableMembers());
    }

    /** Return reachable witness names. */
    public List<String> getReachableWitnessNames()
    {
        return clusterMembersToNames(getReachableWitnesses());
    }

    // Conversion routine.
    private List<String> clusterMembersToNames(Collection<ClusterMember> members)
    {
        ArrayList<String> list = new ArrayList<String>(members.size());
        for (ClusterMember member : members)
        {
            list.add(member.getName());
        }
        return list;
    }

    /**
     * Test to see if we have a valid quorum set. This checks a number of
     * conditions that if violated indicate that the manager is either
     * misconfigured or group communications is misbehaving, which in turn could
     * lead to an invalid computation of quorum.
     * 
     * @return Returns true if the
     */
    public boolean isValidQuorumSet(boolean verbose)
    {
        if (configuredMembers.size() == 0)
        {
            // The quorum set must contain at least one configured member.
            if (verbose)
            {
                CLUtils.println("INVALID QUORUM SET: NO CONFIGURED MEMBERS FOUND");
                CLUtils.println("(ENSURE THAT dataservices.properties FILE CONTAINS AT LEAST MEMBER "
                        + name + ")");
            }
            return false;
        }
        else if (viewMembers.size() == 0)
        {
            // The quorum set must contain at least one member in the GC view.
            if (verbose)
            {
                CLUtils.println("INVALID QUORUM SET: GROUP COMMUNICATION VIEW CONTAINS NO MEMBERS");
                CLUtils.println("(GROUP COMMUNICATIONS MAY BE MISCONFIGURED OR BLOCKED BY A FIREWALL)");
            }
            return false;
        }
        else if (quorumSet.get(name) == null)
        {
            // The quorum set must contain the current member.
            if (verbose)
            {
                CLUtils.println("INVALID QUORUM SET: THIS MEMBER " + name
                        + " IS NOT LISTED");
                CLUtils.println("(GROUP COMMUNICATIONS MAY BE MISCONFIGURED OR BLOCKED BY A FIREWALL; MEMBER NAME MAY BE MISSING FROM dataservices.properties)");
            }
            return false;
        }
        else if (!quorumSet.get(name).isInView())
        {
            // The member must be in the group communications view.
            if (verbose)
            {
                CLUtils.println("INVALID QUORUM SET: THIS MEMBER " + name
                        + " IS NOT LISTED IN THE GROUP COMMUNICATION VIEW");
                CLUtils.println("(GROUP COMMUNICATIONS MAY BE MISCONFIGURED OR BLOCKED BY A FIREWALL)");
            }
            return false;
        }
        else
        {
            // This quorum set appears valid.
            return true;
        }
    }

    /**
     * Determines whether the local manager is in a primary partition, based on
     * validated membership information passed in when this class is
     * instantiated. A manager is in a primary partition if one of the following
     * conditions is met.
     * <ul>
     * <li>The quorum set is one and contains the current member</li>
     * <li>The quorum set contains a simple majority of validated members</li>
     * <li>The quorum set contains an even number of validated members with
     * reachable witness hosts (all must be reachable)</li>
     * </ul>
     * If none of the above obtains, the manager is not in a primary partition.
     * 
     * @param verbose Logs information about how the determination is being
     *            made.
     * @return true if we are in a primary partition
     */
    public boolean isInPrimaryPartition(boolean verbose)
    {
        // Print a message to explain what we are doing.
        if (verbose)
        {
            CLUtils.println("CHECKING FOR QUORUM...");
            CLUtils.println("QUORUM SET MEMBERS ARE: "
                    + CLUtils.iterableToCommaSeparatedList(getQuorumSetNames()));
            CLUtils.println("SIMPLE MAJORITY SIZE: "
                    + this.getSimpleMajoritySize());
            CLUtils.println("VALIDATED MEMBERS ARE: "
                    + CLUtils
                            .iterableToCommaSeparatedList(getValidatedMemberNames()));
            CLUtils.println("REACHABLE MEMBERS ARE: "
                    + CLUtils
                            .iterableToCommaSeparatedList(getReachableMemberNames()));
            CLUtils.println("WITNESS HOSTS ARE: "
                    + CLUtils.iterableToCommaSeparatedList(witnessMembers));
            CLUtils.println("REACHABLE WITNESSES ARE: "
                    + CLUtils
                            .iterableToCommaSeparatedList(getReachableWitnessNames()));
        }

        // Ensure the quorum set is valid.
        if (!this.isValidQuorumSet(verbose))
        {
            CLUtils.println("UNABLE TO ESTABLISH MAJORITY DUE TO INVALID QUORUM SET");
            return false;
        }

        // If we have a valid quorum set with a single validated member, then we
        // have a primary partition.
        if (quorumSet.size() == 1 && validated == 1)
        {
            CLUtils.println("I AM IN A PRIMARY PARTITION AS THERE IS A SINGLE VALIDATED MEMBER IN THE QUORUM SET");
            return true;
        }

        // If we have a simple majority of validated members in the quorum set,
        // then we have a primary partition.
        int simpleMajority = this.getSimpleMajoritySize();
        if (validated >= simpleMajority)
        {
            CLUtils.println(String
                    .format("I AM IN A PRIMARY PARTITION OF %d MEMBERS OUT OF THE REQUIRED MAJORITY OF %d",
                            validated, simpleMajority));
            return true;
        }

        // If we have an even quorum set, a reachable witness host and at
        // least half the quorum members are validated, then we
        // have a primary partition.
        int halfQuorum = quorumSet.size() / 2;
        if ((halfQuorum * 2) == quorumSet.size())
        {
            boolean witnessesOk = witnessSet.size() > 0
                    && (witnessSet.size() == reachableWitnesses);
            if (witnessesOk)
            {
                CLUtils.println(String
                        .format("I AM IN A PRIMARY PARTITION OF %d MEMBERS OUT OF THE REQUIRED MAJORITY OF %d PLUS %d REACHABLE WITNESSES",
                                reachable, halfQuorum, reachableWitnesses));
                return true;
            }
        }

        // We cannot form a quorum. Provide an explanation if desired.
        if (verbose)
        {
            CLUtils.println(String
                    .format("I AM IN A NON-PRIMARY PARTITION OF %d MEMBERS OUT OF A REQUIRED MAJORITY SIZE OF %d",
                            validated, getSimpleMajoritySize()));
        }
        return false;
    }

    /**
     * Returns true if the group membership is valid, which is the case if the
     * following conditions obtain:
     * <ul>
     * <li>There is at least 1 member in the group</li>
     * <li>All individual members in the group are validated through a ping</li>
     * </ul>
     */
    public boolean isValidMembership(boolean verbose)
    {
        if (viewMembers.size() > 0
                && viewMembers.size() == getValidatedMembers().size())
        {
            if (verbose)
            {
                CLUtils.println("MEMBERSHIP IS VALID");
                CLUtils.println("GC VIEW OF CURRENT MEMBERS IS: "
                        + CLUtils.iterableToCommaSeparatedList(viewMembers));
                CLUtils.println("VALIDATED CURRENT MEMBERS ARE: "
                        + CLUtils
                                .iterableToCommaSeparatedList(getValidatedMemberNames()));
            }
            return true;
        }

        if (verbose)
        {
            CLUtils.println("MEMBERSHIP IS NOT VALID");
            CLUtils.println("GC VIEW OF CURRENT MEMBERS IS: "
                    + CLUtils.iterableToCommaSeparatedList(viewMembers));
            CLUtils.println("VALIDATED CURRENT MEMBERS ARE: "
                    + CLUtils
                            .iterableToCommaSeparatedList(getValidatedMemberNames()));
        }
        return false;
    }
}