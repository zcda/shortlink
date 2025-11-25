public class admin_test {

    private static String sql = """
            CREATE TABLE `t_user_%d` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
              `username` varchar(256) NOT NULL COMMENT '用户名',
              `password` varchar(512) NOT NULL COMMENT '密码',
              `real_name` varchar(256) DEFAULT NULL,
              `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
              `mail` varchar(512) DEFAULT NULL,
              `deletion_time` bigint(20) DEFAULT NULL COMMENT '注销时间戳',
              `create_time` datetime DEFAULT NULL,
              `update_time` datetime DEFAULT NULL,
              `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除表示 0:未删除,1:已经删除',
              PRIMARY KEY (`id`),
              UNIQUE KEY `idx_unique_username` (`username`)
            ) ENGINE=InnoDB AUTO_INCREMENT=1992518802287403010 DEFAULT CHARSET=utf8mb4;
            """;

    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.printf((sql) + "%n",i);
        }

    }
}
