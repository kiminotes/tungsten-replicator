The Tungsten Replicator cookbook

(C) Continuent, Inc, 2012,2013
Released under the New BSD license. 
See http://code.google.com/p/tungsten-toolbox/ for more information.

OVERVIEW

This is a collection of recipes to install several replication topologies using Tungsten Replicator.

The following topologies are supported

* master/slave (one master, many slaves)
* fan-in (many masters, one slave)
* all-masters (many masters, with slave services for all nodes)
* star schema (many masters, interconnected through a hub)

GETTING STARTED:

1) unpack the Tungsten tarball
2) Look inside $cookbook_dir (./cookbook) and edit $cookbook_dir/COMMON_NODES.sh file, with the list of your nodes
3) If you are using a number of nodes other than 4, in the NODES_xxxxxx file, 
   a) add as many nodes you are using
   b) check the variables ALL_NODES, MASTERS, and SLAVES, and make sure that they list existing nodes
   c) if you are using less than 4 nodes, comment out the extra ones
4) edit $cookbook_dir/USER_VALUES.sh
5) Optionally, but recommended: VALIDATE your cluster, i.e check that the installer can work fine in them.
   Run $cookbook_dir/validate_cluster
   This command does not install anything, but will perform all healthy checks. Thus you will know if your system
   is fit to install Tungsten.
   If anything goes wrong and you want to see more detail, repeat the validation in debug mode:
   VERBOSE=2 $cookbook_dir/validate_cluster
6) Run the command corresponding to the topology you want to install. The command must run from the directory above the cookbook, i.e the directory from where you can access ./tools/tungsten-replicator.
For example:
   $cookbook_dir/install_master_slave
   $cookbook_dir/install_all_masters
   $cookbook_dir/install_fan_in
   $cookbook_dir/install_star


TAKING ADVANTAGE OF THE NEW TPM INSTALLATION (default with Tungsten Replicator 2.2.0)
Tungsten Replicator, as of 2.1.1-90, ships with the ability of installing using the tpm (Tungsten Package Manager). The main advantages of tpm compared to tungsten-installer are:
* generally faster
* simpler syntax for multi-master (1 command instead of many)
* parallel execution (results in multi-master deployments 10 times quicker)
* more flexibility when updating the cluster.

To install using tpm, simply enable the variable USE_TPM (not needed with Tungsten Replicator 2.2.0)

export USE_TPM=1
./cookbook/install_all_masters

AFTER INSTALLATION

There are a few tools that come together with the cookbook.
As a static bonus, you get a log of the installation, named $cookbook_dir/current_install.log. That file says the installation type, the date and time of the installation, and the commands that were executed to install the cluster.

You have also some more active tools. There is a show_cluster command that display the status of the cluster

* $cookbook_dir/show_cluster

And a similar command that checks the replication flow across the cluster

* $cookbook_dir/test_cluster

And finally a command that remove the installed replicators and clean up the databases.

* $cookbook_dir/clear_cluster

MONITORING AND MAINTENANCE TOOLS

The cookbook includes several shortcuts to the installed tools
* trepctl
* thl
* replicator
For each one of the above, there is a corresponding $cookbook_dir/$TOOL_NAME

e.g.: $cookbook_dir/trepctl status

Similarly, there are maintenance scripts that give you quick access to the configuration and logs
* log        shows the replicator log using 'less'
* show_log   same as 'log'
* vilog      shows the log using 'vi'
* vimlog     shows the log using 'vim'
* conf       shows the configuration files using 'less'
* vimconf    edit the configuration files using 'vim'
* edit_conf  same as 'vimconf'
* heartbeat  Runs 'trepctl heartbeat' in every master in the cluster
* services   Runs 'trepctl services'
* paths      Shows the path of the most used tools and services 
* backups    Shows which backups were taken for each node in the cluster

e.g.: $cookbook_dir/vimlog

TESTING ALL TOPOLOGIES
If you want to test all topologies in one go, after you have updated COMMON_NODES.sh (and eventually all the NODES_*.sh files if you are using more nodes), you can run

$cookbook_dir/test_all_topologies

This command will install, test, and uninstall all topologies, and return the summary of tests results (total, pass, fail).


