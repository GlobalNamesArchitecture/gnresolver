# encoding: UTF-8
# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20160609164952) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "data_sources", force: :cascade do |t|
    t.string   "title",               limit: 255,              null: false
    t.text     "description"
    t.string   "logo_url",            limit: 255
    t.string   "web_site_url",        limit: 255
    t.string   "data_url",            limit: 255
    t.integer  "refresh_period_days",             default: 14
    t.integer  "name_strings_count",              default: 0
    t.string   "data_hash",           limit: 40
    t.integer  "unique_names_count",              default: 0
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "name_string_indices", id: false, force: :cascade do |t|
    t.integer "data_source_id",             null: false
    t.uuid    "name_string_id",             null: false
    t.string  "url",            limit: 255
  end

  add_index "name_string_indices", ["data_source_id"], name: "index_name_string_indices_on_data_source_id", using: :btree
  add_index "name_string_indices", ["name_string_id"], name: "index_name_string_indices_on_name_string_id", using: :btree

  create_table "name_strings", id: :uuid, default: nil, force: :cascade do |t|
    t.uuid     "id_mysql",                   null: false
    t.string   "name",           limit: 255, null: false
    t.uuid     "canonical_uuid"
    t.string   "canonical",      limit: 255
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "name_strings", ["canonical_uuid"], name: "index_name_strings_on_canonical_uuid", using: :btree

end
