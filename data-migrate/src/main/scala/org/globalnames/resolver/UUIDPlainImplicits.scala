package org.globalnames.resolver

import slick.jdbc.{PositionedResult, PositionedParameters, SetParameter}
import java.sql.JDBCType
import java.util.UUID

trait UUIDPlainImplicits {

  implicit class PgPositionedResult(val r: PositionedResult) {
    def nextUUID: UUID = UUID.fromString(r.nextString)

    def nextUUIDOption: Option[UUID] = r.nextStringOption().map(UUID.fromString)
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters): Unit = {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }

}