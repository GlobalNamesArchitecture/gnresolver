require_relative "../environment.rb"
require_relative "gnresolver/version"
require_relative "gnresolver/request"

# Main module for GnResolver web GUI
module GnResolver
  class << self
    attr_writer :logger

    def logger
      @logger ||= Logger.new(STDOUT)
    end
  end
end
