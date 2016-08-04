require "rack-timeout"
require "sinatra"
require "sinatra/base"
require "sinatra/flash"
require "sinatra/redirect_with_flash"
require "better_errors"
require "slim"
require "tilt/sass"

require_relative "lib/gnresolver"
require_relative "routes"
require_relative "helpers"

configure do
  register Sinatra::Flash
  helpers Sinatra::RedirectWithFlash

  use Rack::Timeout, service_timeout: 9_000_000

  set :slim, format: :html
  set :protection, except: :json_csrf
  set :logging, true

  use Rack::MethodOverride
  # use Rack::Session::Cookie, secret: GnResolver.conf.session_secret
end

configure :development do
  use BetterErrors::Middleware
  BetterErrors.application_root = __dir__
end
