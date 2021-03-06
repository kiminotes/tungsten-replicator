class ConfigureDatabasePlatform
  attr_reader :username, :password, :host, :port, :ds_alias
  
  def initialize(host, port, username, password, config, ds_alias)
    @host = host
    @port = port
    @username = username
    @password = password
    @config = config
    @ds_alias = ds_alias
  end
  
  def get_uri_scheme
    raise "Undefined function: #{self.class.name}.get_uri_scheme"
  end
  
  def run(command)
    raise "Undefined function: #{self.class.name}.run"
  end
  
  def get_value(command, column = nil)
    raise "Undefined function: #{self.class.name}.get_value"
  end
	
	def get_extractor_template
    "tungsten-replicator/samples/conf/extractors/#{get_uri_scheme()}.tpl"
	end
	
	def get_applier_template
    "tungsten-replicator/samples/conf/appliers/#{get_uri_scheme()}.tpl"
	end
	
  def enable_extractor_filter_colnames?
    if @config.getProperty(REPL_SVC_EXTRACTOR_FILTER_COLNAMES) == "true"
      true
    else
      false
    end
  end
  
  def enable_extractor_filter_pkey?
    if @config.getProperty(REPL_SVC_EXTRACTOR_FILTER_PKEY) == "true"
      true
    else
      false
    end
  end
  
	def get_extractor_filters()
	  filters = []
    
    if enable_extractor_filter_colnames?
      filters << "colnames"
    end
    
    if enable_extractor_filter_pkey?
      filters << "pkey"
    end
  
    return filters
	end
	
	def get_thl_filters()
	  []
	end
  
  def enable_applier_filter_pkey?
    true
  end
  
  def enable_applier_filter_bidiSlave?
    true
  end
  
  def enable_applier_filter_colnames?
    if @config.getProperty(BATCH_ENABLED) == "true"
      true
    else
      false
    end
  end
	
	def get_applier_filters()
    filters = []
    
    if enable_applier_filter_pkey?
      filters << "pkey"
    end
    
    if enable_applier_filter_bidiSlave?
      filters << "bidiSlave"
    end
    
    if enable_applier_filter_colnames?
      filters << "colnames"
    end
    
    return filters
	end
	
	def get_backup_agents()
	  agent = @config.getProperty(REPL_BACKUP_METHOD)
	  
	  if agent == "none"
	    []
	  else
	    [agent]
	  end
	end
	
	def get_default_backup_agent()
	  agents = get_backup_agents()
	  
	  if agents.size > 0
	    agents[0]
	  else
	    ""
	  end
	end
	
	def get_thl_uri
	  raise "Undefined function: #{self.class.name}.get_thl_uri"
	end
	
	def check_thl_schema(thl_schema)
    raise "Currently unable to check for the THL schema in #{get_uri_scheme}"
  end
  
  def getBasicJdbcUrl()
    raise "Undefined function: #{self.class.name}.getBasicJdbcUrl"
  end
  
  def getJdbcUrl()
    raise "Undefined function: #{self.class.name}.getJdbcUrl"
  end
  
  def getJdbcDriver()
    raise "Undefined function: #{self.class.name}.getJdbcDriver"
  end
  
  def getJdbcScheme()
    getVendor()
  end
  
  def getVendor()
    raise "Undefined function: #{self.class.name}.getVendor"
  end
  
  def getVersion()
    ""
  end
  
  def get_default_master_log_directory
    raise "Undefined function: #{self.class.name}.get_default_master_log_directory"
  end
  
  def get_default_master_log_pattern
    raise "Undefined function: #{self.class.name}.get_default_master_log_pattern"
  end
  
  def get_default_port
    raise "Undefined function: #{self.class.name}.get_default_port"
  end
  
  def get_default_start_script
    raise "Undefined function: #{self.class.name}.get_default_start_script"
  end
  
  def get_default_backup_method
    "none"
  end
  
  def get_valid_backup_methods
    "none|script"
  end
  
  def get_connection_summary(password = true)
    if password == false
      password = ""
    elsif @password.to_s() == ""
      password = " (NO PASSWORD)"
    else
      password = " (WITH PASSWORD)"
    end
    
    "#{@username}@#{@host}:#{@port}#{password}"
  end
  
  def get_applier_key(key)
    [DATASOURCES, @config.getProperty(REPL_DATASOURCE), key]
  end
  
  def get_extractor_key(key)
    if @config.getProperty(REPL_ROLE) == REPL_ROLE_DI
      [DATASOURCES, @config.getProperty(REPL_MASTER_DATASOURCE), key]
    else
      get_applier_key(@config, key)
    end
  end
  
  def get_batch_load_template
    "LOAD DATA INFILE '%%FILE%%' REPLACE INTO TABLE %%TABLE%% CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'"
  end
  
  def get_batch_insert_template
    "INSERT INTO %%BASE_TABLE%%(%%BASE_COLUMNS%%) SELECT %%BASE_COLUMNS%% FROM %%STAGE_TABLE%%"
  end
  
  def get_batch_delete_template
    "DELETE FROM %%BASE_TABLE%% WHERE %%BASE_PKEY%% IN (SELECT %%STAGE_PKEY%% FROM %%STAGE_TABLE%%)"
  end
  
  def get_replication_schema
    "tungsten_${service.name}"
  end
  
  def get_default_table_engine
    "innodb"
  end
  
  def get_allowed_table_engines
    ["innodb"]
  end
  
  def self.build(scheme, host, port, username, password, config, ds_alias)
    klass = self.get_class(scheme)
    return klass.new(host, port, username, password, config, ds_alias)
  end
  
  def self.get_class(scheme)
    self.get_classes().each{
      |kscheme, klass|
      
      if kscheme == scheme
        return klass
      end
    }
    
    raise "Unable to find a database type class for #{scheme}"
  end
  
  def self.get_classes
    unless @database_classes
      @database_classes = {}

      self.subclasses.each{
        |klass|
        o = klass.new(nil, nil, nil, nil, nil, nil)
        @database_classes[o.get_uri_scheme()] = klass
      }
    end
    
    @database_classes
  end
  
  def self.get_types
    return self.get_classes().keys()
  end
  
  def self.inherited(subclass)
    @subclasses ||= []
    @subclasses << subclass
  end
  
  def self.subclasses
    @subclasses
  end
end