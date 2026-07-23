package com.marcowong.motoman;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Pixmap;
import com.marcowong.motoman.ModelData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.marcowong.motoman.track.TrackDirection;
import com.marcowong.motoman.track.TrackSegment;

public class Rider {
    public com.marcowong.motoman.track.logic.Rider logic;

	private final static int skeHead = 0;
	private final static int skeChest = 1;
	private final static int skeWaist = 2;
	private final static int skeHip = 3;
	private final static int skeArmUpperL = 4;
	private final static int skeArmLowerL = 5;
	private final static int skeArmUpperR = 6;
	private final static int skeArmLowerR = 7;
	private final static int skeLegUpperL = 8;
	private final static int skeLegLowerL = 9;
	private final static int skeLegUpperR = 10;
	private final static int skeLegLowerR = 11;
	
	private final static Vector3 posHip = new Vector3(0, 0, 0);
	private final static Vector3 posWaist = new Vector3(0, 1.05f, 0.78f);
	private final static Vector3 posChest = new Vector3(0, 1.68f, 1.49f);
	private final static Vector3 posHead = new Vector3(0, 2.45f, 2.41f);
	private final static Vector3 posArmRoot = new Vector3(1, 2.1f, 2.03f);
	private final static Vector3 posArmUpper = new Vector3(1.19f, 0.68f, 3.24f);
	private final static Vector3 posLegRoot = new Vector3(0.5f, -0.1f, -0.14f);
	private final static Vector3 posLegUpper = new Vector3(0.75f, -1.63f, 2.27f);
	
	private final static Vector3 offHip = new Vector3(posHip);
	private final static Vector3 offWaist = new Vector3(posWaist).sub(posHip);
	private final static Vector3 offChest = new Vector3(posChest).sub(posWaist);
	private final static Vector3 offHead = new Vector3(posHead).sub(posChest);
	private final static Vector3 offArmUpperR = new Vector3(-posArmRoot.x, posArmRoot.y, posArmRoot.z).sub(posChest);
	private final static Vector3 offArmLowerR = new Vector3(-posArmUpper.x, posArmUpper.y, posArmUpper.z).sub(-posArmRoot.x, posArmRoot.y, posArmRoot.z);
	private final static Vector3 offArmUpperL = new Vector3(posArmRoot).sub(posChest);
	private final static Vector3 offArmLowerL = new Vector3(posArmUpper).sub(posArmRoot);
	private final static Vector3 offLegUpperR = new Vector3(-posLegRoot.x, posLegRoot.y, posLegRoot.z).sub(posHip);
	private final static Vector3 offLegLowerR = new Vector3(-posLegUpper.x, posLegUpper.y, posLegUpper.z).sub(-posLegRoot.x, posLegRoot.y, posLegRoot.z);
	private final static Vector3 offLegUpperL = new Vector3(posLegRoot).sub(posHip);
	private final static Vector3 offLegLowerL = new Vector3(posLegUpper).sub(posLegRoot);
	
	private Matrix4 dynHip = new Matrix4();
	private Matrix4 dynWaist = new Matrix4();
	private Matrix4 dynChest = new Matrix4();
	private Matrix4 dynHead = new Matrix4();
	private Matrix4 dynArmUpperL = new Matrix4();
	private Matrix4 dynArmLowerL = new Matrix4();
	private Matrix4 dynArmUpperR = new Matrix4();
	private Matrix4 dynArmLowerR = new Matrix4();
	private Matrix4 dynLegUpperL = new Matrix4();
	private Matrix4 dynLegLowerL = new Matrix4();
	private Matrix4 dynLegUpperR = new Matrix4();
	private Matrix4 dynLegLowerR = new Matrix4();
	
	public Motorcycle motorcycle;
	private Track track;
	
	private FloatBuffer modelSkeMatsFBuf;
    
	private static ModelData model;
	private static IMeshContext modelMeshContext;
	private static Matrix4 pos = new Matrix4();
	private static Matrix4 scaleMat = new Matrix4();
	private static float detachedHeight;

	public static void initResource() {
		model = new ObjLoaderEx().loadObj(Gdx.files.internal("data/rider.obj"), true);
		Pixmap riderSkeletonMapping = new Pixmap(Gdx.files.internal("data/rider.skeleton.png"));
		new ObjLoaderSkeletonPatcher().patch(model, riderSkeletonMapping);
		riderSkeletonMapping.dispose();
		StaticModelTextureFilterConfigManager.add(model);
		modelMeshContext = MeshOptimized.globalStaticMesh.add(model);
		
		pos.translate(0, 0.35f, -0.05f);
		scaleMat.scale(0.55f, 0.55f, 0.55f);
		
		detachedHeight = 0.25f;
    }

	public Rider(Track track) {
		this.track = track;
        this.logic = new com.marcowong.motoman.track.logic.Rider(track.logic);
		modelSkeMatsFBuf = ByteBuffer.allocateDirect(12 * 16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

    private void copyMat(com.marcowong.motoman.track.math.Matrix4 src, Matrix4 dst) {
        System.arraycopy(src.val, 0, dst.val, 0, 16);
    }

    private Matrix4 tmpMatP1 = new Matrix4();
    private Matrix4 tmpMatP2 = new Matrix4();
    private Matrix4 tmpMatP3 = new Matrix4();
    private Matrix4 tmpMatP4 = new Matrix4();
    private Matrix4 tmpMatP5 = new Matrix4();
    private Matrix4 tmpMatP6 = new Matrix4();
    private Matrix4 tmpMatP7 = new Matrix4();
    private Matrix4 tmpMatP8 = new Matrix4();
    private Matrix4 tmpMatP9 = new Matrix4();
    private Matrix4 tmpMatP10 = new Matrix4();
    private Matrix4 tmpMatP11 = new Matrix4();
    private Matrix4 tmpMatP12 = new Matrix4();

	private Matrix4 tmpMat = new Matrix4();
	private Matrix4 tmpMat2 = new Matrix4();

	public void render(ShaderProgram shader, Camera camera) {
		com.marcowong.motoman.track.logic.Rider.Pose pose = logic.state.pose;
        copyMat(pose.matHip, tmpMatP1);
        copyMat(pose.matWaist, tmpMatP2);
        copyMat(pose.matChest, tmpMatP3);
        copyMat(pose.matHead, tmpMatP4);
        copyMat(pose.matLegUpperR, tmpMatP5);
        copyMat(pose.matLegLowerR, tmpMatP6);
        copyMat(pose.matLegUpperL, tmpMatP7);
        copyMat(pose.matLegLowerL, tmpMatP8);
        copyMat(pose.matArmUpperR, tmpMatP9);
        copyMat(pose.matArmLowerR, tmpMatP10);
        copyMat(pose.matArmUpperL, tmpMatP11);
        copyMat(pose.matArmLowerL, tmpMatP12);
		
		dynHip.set(tmpMatP1).trn(offHip);
		tmpMat.idt().trn(-posHip.x, -posHip.y, -posHip.z).mul(dynHip);
		modelSkeMatsFBuf.position(skeHip * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP2).trn(offWaist);
		dynWaist.set(dynHip).mul(tmpMat);
		tmpMat.set(dynWaist).translate(-posWaist.x, -posWaist.y, -posWaist.z);
		modelSkeMatsFBuf.position(skeWaist * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP3).trn(offChest);
		dynChest.set(dynWaist).mul(tmpMat);
		tmpMat.set(dynChest).translate(-posChest.x, -posChest.y, -posChest.z);
		modelSkeMatsFBuf.position(skeChest * 16);
		modelSkeMatsFBuf.put(tmpMat.val);

		tmpMat.set(tmpMatP4).trn(offHead);
		dynHead.set(dynChest).mul(tmpMat);
		tmpMat.set(dynHead).translate(-posHead.x, -posHead.y, -posHead.z);
		modelSkeMatsFBuf.position(skeHead * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP5).trn(offLegUpperR);
		dynLegUpperR.set(dynHip).mul(tmpMat);
		tmpMat.set(dynLegUpperR).translate(posLegRoot.x, -posLegRoot.y, -posLegRoot.z);
		modelSkeMatsFBuf.position(skeLegUpperR * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP6).trn(offLegLowerR);
		dynLegLowerR.set(dynLegUpperR).mul(tmpMat);
		tmpMat.set(dynLegLowerR).translate(posLegUpper.x, -posLegUpper.y, -posLegUpper.z);
		modelSkeMatsFBuf.position(skeLegLowerR * 16);
		modelSkeMatsFBuf.put(tmpMat.val);

		tmpMat.set(tmpMatP7).trn(offLegUpperL);
		dynLegUpperL.set(dynHip).mul(tmpMat);
		tmpMat.set(dynLegUpperL).translate(-posLegRoot.x, -posLegRoot.y, -posLegRoot.z);
		modelSkeMatsFBuf.position(skeLegUpperL * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP8).trn(offLegLowerL);
		dynLegLowerL.set(dynLegUpperL).mul(tmpMat);
		tmpMat.set(dynLegLowerL).translate(-posLegUpper.x, -posLegUpper.y, -posLegUpper.z);
		modelSkeMatsFBuf.position(skeLegLowerL * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP9).trn(offArmUpperR);
		dynArmUpperR.set(dynChest).mul(tmpMat);
		tmpMat.set(dynArmUpperR).translate(posArmRoot.x, -posArmRoot.y, -posArmRoot.z);
		modelSkeMatsFBuf.position(skeArmUpperR * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP10).trn(offArmLowerR);
		dynArmLowerR.set(dynArmUpperR).mul(tmpMat);
		tmpMat.set(dynArmLowerR).translate(posArmUpper.x, -posArmUpper.y, -posArmUpper.z);
		modelSkeMatsFBuf.position(skeArmLowerR * 16);
		modelSkeMatsFBuf.put(tmpMat.val);

		tmpMat.set(tmpMatP11).trn(offArmUpperL);
		dynArmUpperL.set(dynChest).mul(tmpMat);
		tmpMat.set(dynArmUpperL).translate(-posArmRoot.x, -posArmRoot.y, -posArmRoot.z);
		modelSkeMatsFBuf.position(skeArmUpperL * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		tmpMat.set(tmpMatP12).trn(offArmLowerL);
		dynArmLowerL.set(dynArmUpperL).mul(tmpMat);
		tmpMat.set(dynArmLowerL).translate(-posArmUpper.x, -posArmUpper.y, -posArmUpper.z);
		modelSkeMatsFBuf.position(skeArmLowerL * 16);
		modelSkeMatsFBuf.put(tmpMat.val);
		
		shader.setUniformMatrix4fv("skeletonmat", modelSkeMatsFBuf, 12, false);
		
		if (logic.state.attached) {
            Matrix4 motPos = new Matrix4(); copyMat(motorcycle.logic.state.pos, motPos);
            Matrix4 motLean = new Matrix4(); copyMat(motorcycle.logic.state.lean, motLean);

			tmpMat2.set(motPos);
			tmpMat2.translate(0, motorcycle.getLeanHeightShift(), 0);
			tmpMat2.mul(motLean);
			tmpMat2.mul(motorcycle.ridePos);
			tmpMat2.mul(pos);
			tmpMat2.mul(scaleMat);
			
			tmpMat.set(camera.combined);
			tmpMat.mul(tmpMat2);
			shader.setUniformMatrix("modelviewproj", tmpMat);
			tmpMat.set(camera.view);
			tmpMat.mul(tmpMat2);
			shader.setUniformMatrix("modelview", tmpMat);
		} else {
            Matrix4 detPos = new Matrix4(); copyMat(logic.state.detachedPos, detPos);
			tmpMat.set(camera.combined);
			tmpMat.mul(detPos);
			tmpMat.mul(scaleMat);
			shader.setUniformMatrix("modelviewproj", tmpMat);
			tmpMat.set(camera.view);
			tmpMat.mul(detPos);
			tmpMat.mul(scaleMat);
			shader.setUniformMatrix("modelview", tmpMat);
		}
		modelMeshContext.render(shader);
	}

	public void setPersist(boolean b) {
		logic.setPersist(b);
	}

	public void update(float delta) {
        logic.update(delta);
	}

	public boolean isKneeDragging() {
		return logic.isKneeDragging();
	}

	public void attach() {
		logic.attach();
	}
	
	public void detach() {
		logic.detach();
	}

	public float getStrength() {
		return logic.state.strength;
	}
	
	public void setStrength(float strength) {
		logic.state.strength = (strength);
	}
	
	public void dispose() {
	}
}
