require "json"
require "rest_client"
require "ostruct"

# Main module for GnResolver web GUI
module GnResolver
  ENVIRONMENTS = %i(development test production).freeze

  class << self
    def conf
      @conf ||= lambda do
        conf = conf_default.each_with_object({}) do |h, obj|
          obj[h[0]] = conf_file[h[0]] ? conf_file[h[0]] : h[1]
        end
        OpenStruct.new(conf)
      end[]
    end

    private

    def conf_default
      {
        "api_url" => "http://resolver.globalnames.org/name_resolvers.json"
      }
    end

    def conf_file
      @conf_file ||= lambda do
        path = File.join(__dir__, "config", "config.json")
        File.exist?(path) ? JSON.parse(File.read(path)) : {}
      end[]
    end
  end
end
