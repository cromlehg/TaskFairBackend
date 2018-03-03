package models

class Media(val mediaPath: String,
            val id: Long,
            val owner: Option[User],
            val path: String,
            val mimeType: Option[String],
            val created: Long) {

  def getLocalPath = mediaPath + "/" + path
    
  def getURLPath = ""
  
}