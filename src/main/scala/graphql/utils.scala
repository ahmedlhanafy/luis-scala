package graphql

import sangria.schema.Context

object utils {
  def updateCtxWithAppMetaData(c: Context[MyContext, _],
                                       applicationId: String,
                                       versionId: String): MyContext = {
    c.ctx
      .copy(applicationId = Some(applicationId), versionId = Some(versionId))(
        applicationRepo = c.ctx.applicationRepo,
        modelRepo = c.ctx.modelRepo,
        utteranceRepo = c.ctx.utteranceRepo
      )
  }
}
