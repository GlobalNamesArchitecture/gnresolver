package org.globalnames.resolver

case class NameStringIndexData(dataSourceId: Int, nameStringId: Int,
                               taxonId: String, rank: String,
                               acceptedTaxonId: String, synonym: String,
                               classificationPath: String,
                               classificationPathIds: String,
                               createdAt: String, updatedAt: String,
                               classificationPathRanks: String)
