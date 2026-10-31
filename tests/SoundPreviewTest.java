package com.livepulse.app;
public final class SoundPreviewTest {
    private static int checks;
    private static void eq(byte[] data,String expected){checks++;if(!SoundBank.previewMime(data).equals(expected))throw new AssertionError(expected);}
    public static void main(String[] args){
        eq("RIFF0000WAVEdata".getBytes(),"audio/wav");eq("OggSdata".getBytes(),"audio/ogg");eq("fLaCdata".getBytes(),"audio/flac");eq("0000ftypM4A ".getBytes(),"audio/mp4");eq("ID3audio".getBytes(),"audio/mpeg");eq(new byte[]{(byte)255,(byte)251},"audio/mpeg");eq(new byte[]{(byte)255,(byte)241},"audio/aac");eq(new byte[]{(byte)255,(byte)249},"audio/aac");eq(new byte[0],"audio/mpeg");eq("RIFF".getBytes(),"audio/mpeg");System.out.println("SoundPreviewTest: "+checks+" verificacoes aprovadas.");
    }
}
