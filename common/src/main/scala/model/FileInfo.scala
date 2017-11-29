package se.nullable.kth.id1212.fileserver.common.model

case class FileInfo(name: String,
                    owner: String,
                    publicRead: Boolean,
                    publicWrite: Boolean) {
  def perms = s"${if (publicRead) 'r' else '-'}${if (publicWrite) 'w' else '-'}"
  override def toString() = s"$name\towner=$owner\tperms=$perms"
}
