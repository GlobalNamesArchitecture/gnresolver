module GnResolver
  # General API request
  class Request
    def initialize
      @agent = RestClient::Resource.new(GnResolver.conf.api_url,
                                        timeout: 9_000_000,
                                        open_timeout: 9_000_000,
                                        connection: "Keep-Alive")
    end

    def get(params)
      res = @gnresolver.post params
      JSON.parse(res, symbolize_names: true)
    rescue RuntimeError
      GnResolver.logger.error("No response for #{params}")
    end
  end
end
