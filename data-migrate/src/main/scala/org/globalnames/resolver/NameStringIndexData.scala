package org.globalnames.resolver

case class NameStringIndexData(dataSourceId: Int, nameStringId: Int,
                               taxonId: Int, rank: String,
                               acceptedTaxonId: Int, synonym: String,
                               classificationPath: String,
                               classificationPathIds: String,
                               createdAt: String, updatedAt: String,
                               classificationPathRanks: String)
