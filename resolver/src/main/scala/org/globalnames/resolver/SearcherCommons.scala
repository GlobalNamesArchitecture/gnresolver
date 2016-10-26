package org.globalnames
package resolver

import resolver.model._
import slick.driver.PostgresDriver.api._

trait SearcherCommons {
  protected val nameStringIndicies = TableQuery[NameStringIndices]
  protected val dataSources = TableQuery[DataSources]

  protected def joinOnDatasources(nameStringsQuery: Query[NameStrings, NameString, Seq]) = {
    val nameStringsQuery1 = nameStringsQuery.filter { !_.surrogate }
    for {
      ns <- nameStringsQuery1
      nsi <- nameStringIndicies.filter { nsi => nsi.nameStringId === ns.id }
      ds <- dataSources.filter { ds => ds.id === nsi.dataSourceId }
    } yield (ns, nsi, ds)
  }
}
