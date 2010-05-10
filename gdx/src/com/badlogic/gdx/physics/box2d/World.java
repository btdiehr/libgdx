package com.badlogic.gdx.physics.box2d;

import java.util.HashMap;
import java.util.Stack;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.JointDef.JointType;
import com.badlogic.gdx.physics.box2d.joints.DistanceJoint;
import com.badlogic.gdx.physics.box2d.joints.FrictionJoint;
import com.badlogic.gdx.physics.box2d.joints.LineJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJoint;
import com.badlogic.gdx.physics.box2d.joints.PulleyJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.WeldJoint;

/**
 * The world class manages all physics entities, dynamic simulation,
 * and asynchronous queries. The world also contains efficient memory
 * management facilities.
 * @author mzechner
 */
public class World 
{
	static
	{
		System.loadLibrary( "gdx" );
	}
	
	/** the address of the world instance **/
	private final long addr;
	
	/** all known bodies **/
	protected final HashMap<Long, Body> bodies = new HashMap<Long, Body>();
	
	/** all known fixtures **/
	protected final HashMap<Long, Fixture> fixtures = new HashMap<Long, Fixture>( );
	
	/** all known joints **/
	protected final HashMap<Long, Joint> joints = new HashMap<Long, Joint>( );
	
	/** Contact filter **/
	protected ContactFilter contactFilter = null;
	
	/** Contact listener **/
	protected ContactListener contactListener = null;

	/** contact edge pool **/
	protected final Stack<ContactEdge> contactEdgePool = new Stack<ContactEdge>( );
	
	/** contact pool **/
	protected final Stack<Contact> contactPool = new Stack<Contact>( );
	
	/**
	 * Construct a world object.
	 * @param gravity the world gravity vector.
	 * @param doSleep improve performance by not simulating inactive bodies.
	 */
	public World( Vector2 gravity, boolean doSleep )
	{
		addr = newWorld( gravity.x, gravity.y, doSleep );
		
		for( int i = 0; i < 1000; i++ )
		{
			contactEdgePool.push( new ContactEdge() );
			contactPool.push( new Contact(this, 0) );
		}
	}
	
	private native long newWorld( float gravityX, float gravityY, boolean doSleep );
	
	/**
	 *  Register a destruction listener. The listener is owned by you and must
	 * remain in scope.
	 */
	public void setDestructionListener(DestructionListener listener)
	{
		
	}

	/**
	 *  Register a contact filter to provide specific control over collision.
	 * Otherwise the default filter is used (b2_defaultFilter). The listener is
	 * owned by you and must remain in scope.
	 */ 
	public void setContactFilter(ContactFilter filter)
	{
		this.contactFilter = filter;
	}

	/**
	 *  Register a contact event listener. The listener is owned by you and must
	 * remain in scope.
	 */
	void setContactListener(ContactListener listener)
	{
		this.contactListener = listener;
	}

	/**
	 *  Create a rigid body given a definition. No reference to the definition
	 * is retained.
	 * @warning This function is locked during callbacks.
	 */
	public Body createBody(BodyDef def)
	{
		Body body = new Body( this, jniCreateBody( addr, 
										def.type.getValue(),
										def.position.x, def.position.y,
										def.angle, 
										def.linearVelocity.x, def.linearVelocity.y,
										def.angularVelocity,
										def.linearDamping,
										def.angularDamping, 
										def.allowSleep,
										def.awake, 
										def.fixedRotation,
										def.bullet,
										def.active,
										def.inertiaScale) );
		this.bodies.put( body.addr, body );
		return body;
	}
	
	private native long jniCreateBody( long addr, 
									   int type, 
									   float positionX, float positionY,
									   float angle,
									   float linearVelocityX, float linearVelocityY,
									   float angularVelocity,
									   float linearDamping,
									   float angularDamping,
									   boolean allowSleep,
									   boolean awake,
									   boolean fixedRotation,
									   boolean bullet,
									   boolean active,
									   float intertiaScale ); 

	/**
	 * Destroy a rigid body given a definition. No reference to the definition
	 * is retained. This function is locked during callbacks.
	 * @warning This automatically deletes all associated shapes and joints.
	 * @warning This function is locked during callbacks.
	 */
	public void destroyBody(Body body)
	{
		jniDestroyBody( body.addr, body.addr );
		this.bodies.remove( body.addr );
		for( int i = 0; i < body.getFixtureList().size(); i++ )
			this.fixtures.remove(body.getFixtureList().get(i).addr);
		for( int i = 0; i < body.getJointList().size(); i++ )
			this.joints.remove(body.getJointList().get(i).joint.addr);		
	}

	private native void jniDestroyBody( long addr, long bodyAddr );
	
	/** 
	 * Create a joint to constrain bodies together. No reference to the definition
	 * is retained. This may cause the connected bodies to cease colliding.
	 * @warning This function is locked during callbacks.
	 */
	public Joint createJoint(JointDef def)
	{
		long jointAddr = jniCreateJoint( addr, def.type.getValue(), def.bodyA.addr, def.bodyB.addr, def.collideConnected);
		Joint joint = null;
		if( def.type == JointType.DistanceJoint )
			joint = new DistanceJoint( this, jointAddr );
		if( def.type == JointType.FrictionJoint )
			joint = new FrictionJoint( this, jointAddr );
		if( def.type == JointType.GearJoint )
			joint = new GearJoint( this, jointAddr );
		if( def.type == JointType.LineJoint )
			joint = new LineJoint( this, jointAddr );
		if( def.type == JointType.MouseJoint )
			joint = new MouseJoint( this, jointAddr );
		if( def.type == JointType.PrismaticJoint )
			joint = new PrismaticJoint( this, jointAddr);
		if( def.type == JointType.PulleyJoint )
			joint = new PulleyJoint( this, jointAddr );
		if( def.type == JointType.RevoluteJoint )
			joint = new RevoluteJoint( this, jointAddr );
		if( def.type == JointType.WeldJoint )
			joint = new WeldJoint( this, jointAddr );
		if( joint != null )
			joints.put( joint.addr, joint );
		JointEdge jointEdgeA = new JointEdge( def.bodyB, joint );
		JointEdge jointEdgeB = new JointEdge( def.bodyA, joint ); 
		joint.jointEdgeA = jointEdgeA;
		joint.jointEdgeB = jointEdgeB;
		def.bodyA.joints.add( jointEdgeA );
		def.bodyB.joints.add( jointEdgeB );
		return joint;
	}
	
	private native long jniCreateJoint( long addr, int type, long bodyA, long bodyB, boolean collideConnected );
	
	

	/**
	 * Destroy a joint. This may cause the connected bodies to begin colliding.
	 * @warning This function is locked during callbacks.
	 */
	public void destroyJoint(Joint joint)
	{
		jniDestroyJoint( addr, joint.addr );
		joints.remove(joint.addr);
		joint.jointEdgeA.other.joints.remove(joint.jointEdgeB);
		joint.jointEdgeB.other.joints.remove(joint.jointEdgeA);
	}
	
	private native void jniDestroyJoint( long addr, long jointAddr );

	/**
	 * Take a time step. This performs collision detection, integration,
	 * and constraint solution.
	 * @param timeStep the amount of time to simulate, this should not vary.
	 * @param velocityIterations for the velocity constraint solver.
	 * @param positionIterations for the position constraint solver.
	 */
	public void step(	float timeStep,
						int velocityIterations,
						int positionIterations)
	{
		jniStep( addr, timeStep, velocityIterations, positionIterations );
	}
	
	private native void jniStep( long addr, float timeStep, int velocityIterations, int positionIterations );

	/**
	 * Call this after you are done with time steps to clear the forces. You normally	 
	 * call this after each call to Step, unless you are performing sub-steps. By default,
	 * forces will be automatically cleared, so you don't need to call this function.
	 * @see SetAutoClearForces
	 */
	public void clearForces()
	{
		jniClearForces(addr);
	}

	private native void jniClearForces(long addr);
	
	/**
	 * Enable/disable warm starting. For testing.
	 */
	public void setWarmStarting(boolean flag)
	{
		jniSetWarmStarting(addr, flag);
	}

	private native void jniSetWarmStarting( long addr, boolean flag );
	
	/**
	 * Enable/disable continuous physics. For testing.
	 */
	public void setContinuousPhysics(boolean flag)
	{
		jniSetContiousPhysics(addr, flag);
	}
	
	private native void jniSetContiousPhysics( long addr, boolean flag );

	/**
	 * Get the number of broad-phase proxies.
	 */
	public int getProxyCount()
	{
		return jniGetProxyCount(addr);
	}
	
	private native int jniGetProxyCount( long addr );

	/**
	 * Get the number of bodies.
	 */
	public int getBodyCount()
	{
		return jniGetBodyCount(addr);
	}
	
	private native int jniGetBodyCount( long addr );

	/**
	 * Get the number of joints.
	 */
	public int getJointCount()
	{
		return jniGetJointcount(addr);
	}
	
	private native int jniGetJointcount( long addr );
	
	/**
	 * Get the number of contacts (each may have 0 or more contact points).
	 */
	public int getContactCount()
	{
		return jniGetContactCount( addr );
	}

	private native int jniGetContactCount( long addr );
	
	/**
	 * Change the global gravity vector.
	 */
	public void setGravity(Vector2 gravity)
	{
		jniSetGravity( addr, gravity.x, gravity.y );
	}
	
	private native void jniSetGravity( long addr, float gravityX, float gravityY );
	
	/**
	 * Get the global gravity vector.
	 */
	final float[] tmpGravity = new float[2];
	final Vector2 gravity = new Vector2( );	
	public Vector2 getGravity()
	{
		jniGetGravity( addr, tmpGravity );
		gravity.x = tmpGravity[0]; gravity.y = tmpGravity[1];
		return gravity;
	}

	private native void jniGetGravity( long addr, float[] gravity ); 
	
	/**
	 * Is the world locked (in the middle of a time step).
	 */	
	public boolean isLocked()
	{
		return jniIsLocked( addr );
	}

	private native boolean jniIsLocked( long addr );
	
	/**
	 *  Set flag to control automatic clearing of forces after each time step.
	 */
	public void setAutoClearForces(boolean flag)
	{
		jniSetAutoClearForces(addr, flag);
	}

	private native void jniSetAutoClearForces( long addr, boolean flag );
	
	/**
	 *  Get the flag that controls automatic clearing of forces after each time step.
	 */
	public boolean getAutoClearForces()
	{
		return jniGetAutoClearForces( addr );
	}
	
	private native boolean jniGetAutoClearForces( long addr );
	
//	/// Query the world for all fixtures that potentially overlap the
//	/// provided AABB.
//	/// @param callback a user implemented callback class.
//	/// @param aabb the query box.
//	void QueryAABB(b2QueryCallback* callback, const b2AABB& aabb) const;
//
//	/// Ray-cast the world for all fixtures in the path of the ray. Your callback
//	/// controls whether you get the closest point, any point, or n-points.
//	/// The ray-cast ignores shapes that contain the starting point.
//	/// @param callback a user implemented callback class.
//	/// @param point1 the ray starting point
//	/// @param point2 the ray ending point
//	void RayCast(b2RayCastCallback* callback, const b2Vec2& point1, const b2Vec2& point2) const;
//
//	/// Get the world body list. With the returned body, use b2Body::GetNext to get
//	/// the next body in the world list. A NULL body indicates the end of the list.
//	/// @return the head of the world body list.
//	b2Body* GetBodyList();
//
//	/// Get the world joint list. With the returned joint, use b2Joint::GetNext to get
//	/// the next joint in the world list. A NULL joint indicates the end of the list.
//	/// @return the head of the world joint list.
//	b2Joint* GetJointList();
//
//	/// Get the world contact list. With the returned contact, use b2Contact::GetNext to get
//	/// the next contact in the world list. A NULL contact indicates the end of the list.
//	/// @return the head of the world contact list.
//	/// @warning contacts are 
//	b2Contact* GetContactList();
	
	public void dispose( )
	{
		jniDispose( addr );
	}
	
	private native void jniDispose( long addr );
	
	public static void main( String[] argv )
	{
		System.loadLibrary( "gdx" );
				
		World world = new World( new Vector2( 0, -10 ), true );
		BodyDef bodyDef = new BodyDef( );
		bodyDef.active = true;
		bodyDef.position.set( 0, 10 );
		bodyDef.type = BodyType.DynamicBody;
		
		CircleShape shape = new CircleShape();
		shape.setRadius(1);		
		
		Body body = world.createBody(bodyDef);
		Fixture fixture = body.createFixture(shape, 1);
		fixture.setRestitution(0.5f);
		shape.dispose();
		
		bodyDef.position.set( 0, -1 );
		bodyDef.type = BodyType.StaticBody;
		
		PolygonShape pShape = new PolygonShape();
		pShape.setAsBox( 2, 1 );
		Body body2 = world.createBody(bodyDef);
		body2.createFixture(pShape, 1);
		pShape.dispose();
		
		body.setUserData( "circle" );
		body2.setUserData( "ground" );
		
		world.setContactFilter( new ContactFilter() {
			
			@Override
			public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) 
			{
				System.out.println( "contact between " + fixtureA.getBody().getUserData() + " and " + fixtureB.getBody().getUserData() );
				return true;
			}
		});
		
		world.setContactListener( new ContactListener() 
		{
			
			@Override
			public void endContact(Contact contact) 
			{
				System.out.println( "ending contact " + contact.getFixtureA().getBody().getUserData() + ": " + contact.getFixtureB().getBody().getUserData());				
			}
			
			@Override
			public void beginContact(Contact contact) 
			{			
				System.out.println( "beginning contact " + contact.getFixtureA().getBody().getUserData() + ": " + contact.getFixtureB().getBody().getUserData());
			}
		});
		
		for( int i = 0; i < 60*3; i++)
		{
			world.step( 1 / 60.0f, 1, 1 );
//			System.out.println(body.getWorldCenter());
		}				
	}
	
	/**
	 * Internal method called from JNI in case a contact happens
	 * @param fixtureA
	 * @param fixtureB
	 * @return
	 */
	private boolean contactFilter( long fixtureA, long fixtureB )
	{
		if( contactFilter != null )
			return contactFilter.shouldCollide( fixtures.get(fixtureA), fixtures.get(fixtureB));
		else
			return true;
	}
		
	private void beginContact( long contactAddr )
	{
		Contact contact = null;
		if( contactPool.size() == 0 )
			contact = new Contact( this, contactAddr );
		else
			contact = contactPool.pop();
		contact.addr = contactAddr;
		
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		
		ContactEdge contactA = null;
		ContactEdge contactB = null;
		
		if( contactEdgePool.size() == 0 )		
			contactA = new ContactEdge( );
		else
			contactA = contactEdgePool.pop();
		
		if( contactEdgePool.size() == 0 )
			contactB = new ContactEdge( );
		else
			contactB = contactEdgePool.pop();						
		
		contactA.contact = contact;
		contactA.other = bodyB;
		contactB.contact = contact;
		contactB.other = bodyA;
		
		bodyA.contacts.add( contactA );
		bodyB.contacts.add( contactB );
		
		if( contactListener != null )
			contactListener.beginContact( contact );
	}
	
	private void endContact( long contactAddr )
	{
		Contact contact = null;
		if( contactPool.size() == 0 )
			contact = new Contact( this, contactAddr );
		else
			contact = contactPool.pop();
		contact.addr = contactAddr;
		
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
				
		
		if( contactListener != null )
			contactListener.endContact( contact );
	}
}
