package com.marcowong.motoman;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.marcowong.motoman.ModelData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.*;

public class Motorcycle {
	public com.marcowong.motoman.track.logic.Motorcycle logic;
	
	public MotorcycleFX fx = new MotorcycleFX();
	public MotorcycleSFX sfx = new MotorcycleSFX(this, new SFXBackfireReporter());
	
	private Rider _rider;
	public Rider rider; // we need getters/setters here ideally, but making public works if MainMotorcycle uses it
	
	public Matrix4 ridePos = new Matrix4();
	
	public Matrix4 bodyPos = new Matrix4();
	public Matrix4 frontWheelPos = new Matrix4();
	public Matrix4 rearWheelPos = new Matrix4();
	public Vector3 backfirePos = new Vector3();
	
	public float leanAngleMaxWhenRunningRenderHeightShift = 0.07f;
	public float leanAngleMaxWhenCrashedRenderHeightShift = 0.7f;
	
	public float getLeanHeightShift() {
		if (logic.state.isCrashed) {
			return leanAngleMaxWhenCrashedRenderHeightShift;
		} else {
			return Math.abs(logic.state.leanAngle) / logic.leanAngleMaxWhenRunning * leanAngleMaxWhenRunningRenderHeightShift;
		}
	}
	
	public IMeshContext bodyModelMeshContext;
	public IMeshContext frontWheelModelMeshContext;
	public IMeshContext rearWheelModelMeshContext;
	
	private static ModelData shadowModel;
	private static IMeshContext shadowModelMeshContext;
	
	public static void initResource() {
		shadowModel = new ObjLoaderEx().loadObj(Gdx.files.internal("data/bikeShadow.obj"), true);
		StaticModelTextureFilterConfigManager.add(shadowModel);
		shadowModelMeshContext = MeshOptimized.globalStaticMesh.add(shadowModel);
	}
	
	public Motorcycle(Track track, com.marcowong.motoman.track.logic.IMotorcycleInputMeters inputMeters) {
		this.logic = new com.marcowong.motoman.track.logic.Motorcycle(track.logic, inputMeters);
	}

    public void setRider(Rider r) {
        this.rider = r;
        this.logic.rider = r.logic;
    }
	
	private Matrix4 tmpMat2 = new Matrix4();
	private void renderModel(ShaderProgram shader, Camera camera, IMeshContext meshContext, Matrix4 modelPos) {
		tmpMat2.set(camera.combined);
		tmpMat2.mul(modelPos);
		shader.setUniformMatrix("modelviewproj", tmpMat2);
		tmpMat2.set(camera.view);
		tmpMat2.mul(modelPos);
		shader.setUniformMatrix("modelview", tmpMat2);
		meshContext.render(shader);
	}
	
	private void copyMat(com.marcowong.motoman.track.math.Matrix4 src, Matrix4 dst) {
		System.arraycopy(src.val, 0, dst.val, 0, 16);
	}
	
	private Vector3 tmpVec4 = new Vector3();
	private Matrix4 tmpMat = new Matrix4();
	private Matrix4 tmpMat3 = new Matrix4();
	private Matrix4 statePos = new Matrix4();
	private Matrix4 stateLean = new Matrix4();
	private Matrix4 stateFrontWheelRot = new Matrix4();
	private Matrix4 stateRearWheelRot = new Matrix4();
	
	public void render(ShaderProgram shader, Camera camera) {
		copyMat(logic.state.pos, statePos);
		copyMat(logic.state.lean, stateLean);
		copyMat(logic.state.frontWheelRot, stateFrontWheelRot);
		copyMat(logic.state.rearWheelRot, stateRearWheelRot);
		
		tmpMat3.set(statePos);
		tmpMat3.translate(0, this.getLeanHeightShift(), 0);
		tmpMat3.mul(stateLean);
		
		tmpMat.set(tmpMat3);
		tmpMat.mul(bodyPos);
		renderModel(shader, camera, bodyModelMeshContext, tmpMat);
		tmpMat.set(tmpMat3);
		tmpMat.mul(frontWheelPos);
		tmpMat.mul(stateFrontWheelRot);
		renderModel(shader, camera, frontWheelModelMeshContext, tmpMat);
		tmpMat.set(tmpMat3);
		tmpMat.mul(rearWheelPos);
		tmpMat.mul(stateRearWheelRot);
		renderModel(shader, camera, rearWheelModelMeshContext, tmpMat);
		
		tmpVec4.set(0, logic.massCenterHeight, 0);
		tmpVec4.mul(stateLean);
		tmpMat.set(statePos);
		tmpMat.translate(tmpVec4.x, 0.01f, 0);
		tmpMat.scale(1 + 0.5f * Math.abs(tmpVec4.x), 1, 1);
		renderModel(shader, camera, shadowModelMeshContext, tmpMat);
	}
	
	public void update(float delta) {
		logic.update(delta);
		if (logic.state == logic.statePersist) {
			updateFX(delta);
			sfx.update(delta);
		}
	}
	
	private float lastBackfireSize = 0;
	private class SFXBackfireReporter implements MotorcycleSFX.BackfireReporter {
		@Override
		public void reportBackfire(float size) {
			lastBackfireSize = size;
		}
	}
	
	private BackfireFXPosition backfireFXPosition = new BackfireFXPosition();
	private class BackfireFXPosition implements MotorcycleFX.DynamicFXPosition {
		@Override
		public void getPosition(Vector3 vec) {
            copyMat(logic.state.pos, statePos);
            copyMat(logic.state.lean, stateLean);
			vec.set(backfirePos);
			vec.mul(stateLean);
			vec.mul(statePos);
		}
	}
	
	private Matrix4 tmpMat5 = new Matrix4();
	private Vector3 tmpVec3 = new Vector3();
	private void updateFX(float delta) {
        copyMat(logic.state.pos, statePos);
        copyMat(logic.state.lean, stateLean);

		float speedRatio = Math.min(1, logic.state.bikeVelo.len2());
		if (logic.state.frontTraction < 1) {
			tmpMat5.set(statePos);
			tmpVec3.set(0, 0, 0);
			tmpVec3.mul(frontWheelPos);
			tmpMat5.translate(tmpVec3.x, 0, tmpVec3.z);
			tmpMat5.getTranslation(tmpVec3);
			fx.addSmoke(((1 - logic.state.frontTraction) * 4 + ((float)Math.random() - 0.5f)) * speedRatio,
					tmpVec3.x + 2 * ((float)Math.random() - 0.5f) * speedRatio,
					tmpVec3.y + (float)Math.random() * speedRatio,
					tmpVec3.z + 2 * ((float)Math.random() - 0.5f) * speedRatio);
		}
		if (logic.state.backTraction < 1) {
			tmpMat5.set(statePos);
			tmpVec3.set(0, 0, 0);
			tmpVec3.mul(rearWheelPos);
			tmpMat5.translate(tmpVec3.x, 0, tmpVec3.z);
			tmpMat5.getTranslation(tmpVec3);
			fx.addSmoke(((1 - logic.state.backTraction) * 4 + ((float)Math.random() - 0.5f)) * speedRatio,
					tmpVec3.x + 2 * ((float)Math.random() - 0.5f) * speedRatio,
					tmpVec3.y + (float)Math.random() * speedRatio,
					tmpVec3.z + 2 * ((float)Math.random() - 0.5f) * speedRatio);
		}
		if (logic.state.isTouchingGround) {
			tmpVec3.set(0, logic.massCenterHeight, 0);
			tmpVec3.mul(stateLean);
			tmpVec3.y = 0;
			tmpVec3.mul(statePos);
			fx.addSpark(((float)Math.random() + 0.5f) * speedRatio,
					tmpVec3.x + 2 * ((float)Math.random() - 0.5f) * speedRatio,
					tmpVec3.y + (float)Math.random() * speedRatio,
					tmpVec3.z + 2 * ((float)Math.random() - 0.5f) * speedRatio);
		}
		if (lastBackfireSize > 0) {
			fx.addBackfire(2 * lastBackfireSize, backfireFXPosition);
			lastBackfireSize = 0;
		}
		fx.update(delta);
	}
	
	public void dispose() {
		fx.dispose();
		sfx.dispose();
	}

    // Needed for GameScreen
    public void setPersist(boolean b) {
        logic.setPersist(b);
    }
}
