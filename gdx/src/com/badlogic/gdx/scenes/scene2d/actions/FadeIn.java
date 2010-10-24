
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.PoolObjectFactory;

public class FadeIn implements Action {
	static final Pool<FadeIn> pool = new Pool<FadeIn>(new PoolObjectFactory<FadeIn>() {
		@Override public FadeIn createObject () {
			return new FadeIn();
		}
	}, 100);

	float startAlpha = 0;
	float deltaAlpha = 0;
	private float duration;
	private float invDuration;
	private float taken = 0;
	private Actor target;
	private boolean done;

	public static FadeIn $ (float duration) {
		FadeIn action = pool.newObject();
		action.duration = duration;
		action.invDuration = 1 / duration;
		return action;
	}

	@Override public void setTarget (Actor actor) {
		this.target = actor;
		this.target.color.a = 0;
		this.startAlpha = 0;
		this.deltaAlpha = 1;
		this.taken = 0;
		this.done = false;
	}

	@Override public void act (float delta) {
		taken += delta;
		if (taken >= duration) {
			taken = duration;
			done = true;
		}

		float alpha = taken * invDuration;
		target.color.a = startAlpha + deltaAlpha * alpha;
	}

	@Override public boolean isDone () {
		return done;
	}

	@Override public void finish () {
		pool.free(this);
	}

	@Override public Action copy () {
		return $(duration);
	}
}
