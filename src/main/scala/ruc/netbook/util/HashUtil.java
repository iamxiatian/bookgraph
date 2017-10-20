package nlp.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * 采用Murmur 3算法实现的哈希处理，由于Murmur在64位和32位的计算机上，对相同内容输出的结果并
 * 不相同（因为优化原因造成），所以，请确保运行的计算机都采用64为计算机。
 * <p>
 * SEE: <a href="https://en.wikipedia.org/wiki/MurmurHash">Murmur algorithm</a>
 *
 * @author Tian Xia Email: xiat@ruc.edu.cn
 *         School of IRM, Renmin University of China.
 *         Jul 24, 2017 18:02
 */
public class HashUtil {
    /**
     * 对一段字节数组生成一个唯一的长整数，作为其摘要结果
     *
     * @param content
     * @return
     */
    public static long hash(byte[] content) {
        return Hashing.murmur3_32().hashBytes(content).asLong();
    }

    /**
     * 对一段文字按照其utf8格式，转换为字节数组并计算哈希值
     * @param content
     * @return
     */
    public static long hash(String content) {
        if(content==null) {
            return 0;
        } else {
            return hash(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
