package indi.aljet.mybackgroundupdate_master.bgupdate;


public interface ProgressListener {

    /**
     * 下载
     * @param section 已经下载或者上传节数
     * @param total 总字节部分
     *
     * @param done  是否完成
     */
    void onProgress(float section,float total,boolean done);
}
