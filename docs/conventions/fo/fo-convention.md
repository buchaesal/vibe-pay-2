# Next.js Frontend 개발 컨벤션

---

## 1. 코딩 규칙

### 1.1 컴포넌트화 (필수)
- 처음부터 컴포넌트로 분리해서 작성
- 한 파일에 모든 코드를 작성하지 말 것

```typescript
// Bad
const Layout = ({ children }) => (
  <div className="layout">
    <header>Header content ...</header>
    <main>{children}</main>
    <footer>Footer content ...</footer>
  </div>
);

// Good
import Header from '@/components/header';
import Footer from '@/components/footer';

const Layout = ({ children }) => (
  <div className="layout">
    <Header />
    <main>{children}</main>
    <Footer />
  </div>
);
```

### 1.2 Nullish Coalescing 사용
- `||` 대신 `??` 사용
- undefined/null만 체크하고 빈 문자열, false 값은 유지

```typescript
const name = foo ?? "default value";  // foo가 null/undefined일 때만 "default value"
```

### 1.3 배열 렌더링 시 key 필수
```typescript
{items.map((item, index) => <li key={index}>{item}</li>)}
```

### 1.4 try-catch로 에러 처리
```typescript
async function fetchData() {
  try {
    const response = await fetch('https://api.example.com/data');
    if (!response.ok) {
      throw new Error(`API failed: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    throw error;
  }
}
```

### 1.5 삼항 연산자
```typescript
{isLogin ? <div>Logged in</div> : <div>Not Logged in</div>}
```

### 1.6 Destructuring
```typescript
// Good
function displayPerson({ name, age, job }) {
  console.log(name);
}
```

### 1.7 Optional Chaining
- 값이 없을 수 있는 경우 `?.` 사용

```typescript
productList?.info?.color
```

---

## 2. 필수 패키지

### 2.1 Zod (스키마 검증)

**설치:**
```bash
pnpm add zod
```

**사용:**
```typescript
import { z } from 'zod';

const productSchema = z.object({
  name: z.string(),
  price: z.number().positive(),
});

type Product = z.infer<typeof productSchema>;  // 타입 자동 생성

const validateProduct = productSchema.safeParse(productJson);

if (!validateProduct.success) {
  console.error(validateProduct.error.message);
  return;
}
```

### 2.2 tailwind-merge + clsx

**설치:**
```bash
pnpm add tailwind-merge clsx
```

**설정:** `src/lib/utils.ts`
```typescript
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

**사용:**
```typescript
import { cn } from "@/lib/utils";

<div className={cn("base-class", conditionalClass && "additional-class")} />
```

---

## 3. 에러 핸들링

### 3.1 Server Action에 try-catch 추가
```typescript
export async function createInvoice(formData: FormData) {
  const { customerId, amount, status } = CreateInvoice.parse({
    customerId: formData.get('customerId'),
    amount: formData.get('amount'),
    status: formData.get('status'),
  })
 
  const amountInCents = amount * 100
  const date = new Date().toISOString().split('T')[0]
 
  try {
    await sql`
      INSERT INTO invoices (customer_id, amount, status, date)
      VALUES (${customerId}, ${amountInCents}, ${status}, ${date})
    `
  } catch (error) {
    return { message: 'Database Error: Failed to Create Invoice.' }
  }
 
  revalidatePath('/dashboard/invoices')
  redirect('/dashboard/invoices')
}
```

### 3.2 error.tsx로 에러 처리
경로 세그먼트에 `error.tsx` 파일 생성:

```typescript
'use client'

import { useEffect } from 'react'

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <main className="flex h-full flex-col items-center justify-center">
      <h2 className="text-center">Something went wrong!</h2>
      <button
        className="mt-4 rounded-md bg-blue-500 px-4 py-2 text-sm text-white"
        onClick={() => reset()}
      >
        Try again
      </button>
    </main>
  )
}
```

### 3.3 notFound()로 404 처리
```typescript
import { notFound } from 'next/navigation'

export default async function Page({ params }: { params: { id: string } }) {
  const invoice = await fetchInvoiceById(params.id)
  
  if (!invoice) {
    notFound()
  }
  
  return <div>{invoice.name}</div>
}
```

`not-found.tsx` 파일 생성:
```typescript
import Link from 'next/link'

export default function NotFound() {
  return (
    <main className="flex h-full flex-col items-center justify-center gap-2">
      <h2 className="text-xl font-semibold">404 Not Found</h2>
      <p>Could not find the requested resource.</p>
      <Link href="/" className="mt-4 rounded-md bg-blue-500 px-4 py-2 text-sm text-white">
        Go Home
      </Link>
    </main>
  )
}
```

**전체 404 처리:** `[...not_found]` 경로에 notFound() 호출

---

## 4. 데이터 페칭

### 4.1 Server Component에서 데이터 페칭
Server Component에서는 `async/await`를 직접 사용.

```typescript
// app/posts/page.tsx
async function getPosts() {
  const res = await fetch('https://api.example.com/posts', {
    cache: 'no-store', // 매번 새로운 데이터
    // cache: 'force-cache', // 기본값, 캐싱
    // next: { revalidate: 3600 } // 1시간마다 재검증
  })
  
  if (!res.ok) {
    throw new Error('Failed to fetch posts')
  }
  
  return res.json()
}

export default async function PostsPage() {
  const posts = await getPosts()
  
  return (
    <ul>
      {posts.map((post) => (
        <li key={post.id}>{post.title}</li>
      ))}
    </ul>
  )
}
```

### 4.2 Client Component에서 데이터 페칭
`useState`와 `useEffect` 사용.

```typescript
'use client'

import { useState, useEffect } from 'react'

export default function PostsPage() {
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function fetchPosts() {
      try {
        const res = await fetch('/api/posts')
        if (!res.ok) throw new Error('Failed to fetch')
        const data = await res.json()
        setPosts(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    
    fetchPosts()
  }, [])

  if (loading) return <div>Loading...</div>
  if (error) return <div>Error: {error}</div>
  
  return (
    <ul>
      {posts.map((post) => (
        <li key={post.id}>{post.title}</li>
      ))}
    </ul>
  )
}
```

### 4.3 API Routes
`app/api/posts/route.ts`:
```typescript
import { NextResponse } from 'next/server'

export async function GET() {
  try {
    const res = await fetch('https://api.example.com/posts')
    const data = await res.json()
    
    return NextResponse.json(data)
  } catch (error) {
    return NextResponse.json(
      { error: 'Failed to fetch posts' },
      { status: 500 }
    )
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json()
    
    const res = await fetch('https://api.example.com/posts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    
    const data = await res.json()
    return NextResponse.json(data)
  } catch (error) {
    return NextResponse.json(
      { error: 'Failed to create post' },
      { status: 500 }
    )
  }
}
```

### 4.4 Server Actions
Form 제출이나 mutation에 사용.

```typescript
// app/actions.ts
'use server'

import { revalidatePath } from 'next/cache'

export async function createPost(formData: FormData) {
  const title = formData.get('title')
  const content = formData.get('content')
  
  try {
    const res = await fetch('https://api.example.com/posts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, content })
    })
    
    if (!res.ok) {
      throw new Error('Failed to create post')
    }
    
    revalidatePath('/posts')
    return { success: true }
  } catch (error) {
    return { success: false, error: error.message }
  }
}
```

**사용:**
```typescript
// app/posts/new/page.tsx
import { createPost } from '@/app/actions'

export default function NewPostPage() {
  return (
    <form action={createPost}>
      <input name="title" type="text" required />
      <textarea name="content" required />
      <button type="submit">Create Post</button>
    </form>
  )
}
```

---

## 5. 상태 관리 (Zustand)

### 5.1 설치
```bash
pnpm add zustand
```

### 5.2 Store 생성
```typescript
import { create } from 'zustand'

const useBearStore = create((set) => ({
  bears: 0,
  increasePopulation: () => set((state) => ({ bears: state.bears + 1 })),
  removeAllBears: () => set({ bears: 0 }),
}))
```

### 5.3 사용
```typescript
function BearCounter() {
  const bears = useBearStore((state) => state.bears)
  return <h1>{bears} around here...</h1>
}

function Controls() {
  const increasePopulation = useBearStore((state) => state.increasePopulation)
  return <button onClick={increasePopulation}>one up</button>
}
```

**특징:**
- Provider 불필요
- 상태 변경 시 자동 리렌더링
- 간단하고 직관적인 API

---

## 6. 파일 및 폴더 구조

### 6.1 컴포넌트 분류
```
src/
├── components/
│   ├── ui/              # 재사용 가능한 기본 UI 컴포넌트
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   └── Modal.tsx
│   ├── layout/          # 레이아웃 컴포넌트
│   │   ├── Header.tsx
│   │   ├── Footer.tsx
│   │   └── Sidebar.tsx
│   └── features/        # 기능별 컴포넌트
│       ├── auth/
│       ├── products/
│       └── cart/
```

### 6.2 네이밍 규칙
- **컴포넌트**: PascalCase (`UserProfile.tsx`)
- **유틸/함수**: camelCase (`formatDate.ts`)
- **상수**: UPPER_SNAKE_CASE (`API_BASE_URL`)
- **폴더**: kebab-case or camelCase

---

## 7. 성능 최적화

### 7.1 이미지 최적화
Next.js Image 컴포넌트 사용 필수.

```typescript
import Image from 'next/image'

<Image
  src="/profile.jpg"
  alt="Profile"
  width={500}
  height={500}
  priority // LCP 이미지에 사용
/>
```

### 7.2 동적 import
필요할 때만 컴포넌트 로드.

```typescript
import dynamic from 'next/dynamic'

const HeavyComponent = dynamic(() => import('@/components/HeavyComponent'), {
  loading: () => <div>Loading...</div>,
  ssr: false // 클라이언트에서만 렌더링
})
```

### 7.3 React.memo 사용
불필요한 리렌더링 방지.

```typescript
import { memo } from 'react'

const ProductCard = memo(({ product }) => {
  return <div>{product.name}</div>
})
```

### 7.4 useCallback, useMemo
```typescript
import { useCallback, useMemo } from 'react'

const Component = ({ items }) => {
  // 함수 메모이제이션
  const handleClick = useCallback(() => {
    console.log('clicked')
  }, [])
  
  // 계산 결과 메모이제이션
  const expensiveValue = useMemo(() => {
    return items.reduce((acc, item) => acc + item.price, 0)
  }, [items])
  
  return <div>{expensiveValue}</div>
}
```

---

## 8. TypeScript 규칙

### 8.1 Props 타입 정의
```typescript
interface ButtonProps {
  children: React.ReactNode
  onClick?: () => void
  variant?: 'primary' | 'secondary'
  disabled?: boolean
}

export default function Button({ 
  children, 
  onClick, 
  variant = 'primary',
  disabled = false 
}: ButtonProps) {
  return <button onClick={onClick}>{children}</button>
}
```

### 8.2 API 응답 타입
```typescript
interface User {
  id: number
  name: string
  email: string
}

interface ApiResponse<T> {
  data: T
  message: string
  success: boolean
}

async function getUser(id: number): Promise<ApiResponse<User>> {
  const res = await fetch(`/api/users/${id}`)
  return res.json()
}
```

### 8.3 any 사용 금지
`any` 대신 `unknown` 또는 구체적인 타입 사용.

```typescript
// Bad
const data: any = await fetchData()

// Good
const data: unknown = await fetchData()
if (isUser(data)) {
  // 타입 가드 사용
}
```